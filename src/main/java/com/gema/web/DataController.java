package com.gema.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class DataController {
    private final Db db;
    public DataController(Db db) { this.db = db; }

    private SessionUser user(HttpSession s) { return (SessionUser) s.getAttribute("user"); }
    private boolean live(HttpSession s) { SessionUser u=user(s); return u!=null && !u.demo() && db.configured(); }

    @GetMapping("/dashboard")
    public Object dashboard(HttpSession s) {
        SessionUser u=user(s); String role=u==null?"COORDINADOR":u.role();
        if (!live(s)) return DemoData.dashboard(role);
        try {
            if (role.equals("ALUMNO")) {
                int id=u.studentId();
                return one("""
                  SELECT
                    (SELECT COUNT(*) FROM tarea t INNER JOIN materia m ON t.docente_materia_materia_id_materia=m.id_materia INNER JOIN alumno a ON a.curso_id_curso=m.curso_id_curso LEFT JOIN entrega e ON e.tarea_id_tarea=t.id_tarea AND e.alumno_id_alumno=a.id_alumno WHERE a.id_alumno=? AND e.id_entrega IS NULL AND t.`fecha_límite`>=CURDATE()) pending,
                    (SELECT COUNT(*) FROM entrega WHERE alumno_id_alumno=?) submitted,
                    (SELECT COUNT(*) FROM entrega WHERE alumno_id_alumno=? AND puntaje IS NOT NULL) graded,
                    COALESCE((SELECT racha_actual FROM racha_alumno WHERE alumno_id=?),0) streak
                  """, id,id,id,id);
            }
            if (role.equals("DOCENTE")) {
                int id=u.teacherId();
                return one("""
                  SELECT
                    (SELECT COUNT(DISTINCT materia_id_materia) FROM docente_materia WHERE docente_id_docente=?) subjects,
                    (SELECT COUNT(*) FROM tarea WHERE docente_materia_docente_id_docente=?) tasks,
                    (SELECT COUNT(*) FROM entrega e INNER JOIN tarea t ON e.tarea_id_tarea=t.id_tarea WHERE t.docente_materia_docente_id_docente=?) submissions,
                    (SELECT COUNT(*) FROM entrega e INNER JOIN tarea t ON e.tarea_id_tarea=t.id_tarea WHERE t.docente_materia_docente_id_docente=? AND e.puntaje IS NULL) pendingGrades
                  """, id,id,id,id);
            }
            return one("""
              SELECT
                (SELECT COUNT(*) FROM alumno WHERE UPPER(estado)='ACTIVO') students,
                (SELECT COUNT(*) FROM docente WHERE UPPER(estado)='ACTIVO') teachers,
                (SELECT COUNT(*) FROM tarea WHERE fecha_publicacion<=NOW()) tasks,
                (SELECT COUNT(*) FROM entrega) submissions
              """);
        } catch(Exception e) { return DemoData.dashboard(role); }
    }

    @GetMapping("/courses")
    public Object courses(HttpSession s) {
        if (!live(s)) return DemoData.courses();
        return safeList("SELECT id_curso, curso, `sección` seccion, `año` FROM curso ORDER BY `año` DESC, id_curso", DemoData.courses());
    }

    @GetMapping("/teachers")
    public Object teachers(HttpSession s) {
        if (!live(s)) return DemoData.teachers();
        return safeList("""
          SELECT d.id_docente,d.nombre,d.apellido,d.`cédula` cedula,d.estado,
                 COALESCE(GROUP_CONCAT(DISTINCT m.nombre ORDER BY m.nombre SEPARATOR ', '),'Sin materias') materias
          FROM docente d LEFT JOIN materia m ON m.docente_id_docente=d.id_docente
          GROUP BY d.id_docente,d.nombre,d.apellido,d.`cédula`,d.estado
          ORDER BY d.nombre,d.apellido
        """, DemoData.teachers());
    }

    @PostMapping("/teachers")
    public ResponseEntity<?> addTeacher(@RequestBody Map<String,Object> b, HttpSession s) {
        if (!live(s)) return ResponseEntity.ok(Map.of("ok",true,"demo",true));
        SessionUser u=user(s); if(u==null || !u.role().equals("COORDINADOR")) return ResponseEntity.status(403).body(Map.of("ok",false));
        String nombre=str(b,"nombre"), apellido=str(b,"apellido"), cedula=str(b,"cedula"), pass=str(b,"password"), estado=strOr(b,"estado","Activo");
        try(Connection c=db.open()) {
            c.setAutoCommit(false);
            int uid;
            try(PreparedStatement ps=c.prepareStatement("INSERT INTO usuario(rol,cedula,nombre,apellido,`contraseña`,estado) VALUES('DOCENTE',?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                ps.setString(1,cedula);ps.setString(2,nombre);ps.setString(3,apellido);ps.setString(4,pass);ps.setString(5,estado);ps.executeUpdate();
                try(ResultSet rs=ps.getGeneratedKeys()){ if(!rs.next()) throw new SQLException("No se generó usuario"); uid=rs.getInt(1); }
            }
            try(PreparedStatement ps=c.prepareStatement("INSERT INTO docente(nombre,apellido,`cédula`,estado,usuario_id_usuario) VALUES(?,?,?,?,?)")){
                ps.setString(1,nombre);ps.setString(2,apellido);ps.setString(3,cedula);ps.setString(4,estado);ps.setInt(5,uid);ps.executeUpdate();
            }
            c.commit(); return ResponseEntity.ok(Map.of("ok",true));
        } catch(Exception e){ return error(e); }
    }

    @PutMapping("/teachers/{id}")
    public ResponseEntity<?> editTeacher(@PathVariable int id,@RequestBody Map<String,Object>b,HttpSession s){
        if(!live(s)) return ResponseEntity.ok(Map.of("ok",true,"demo",true));
        try { db.update("UPDATE docente SET nombre=?,apellido=?,`cédula`=?,estado=? WHERE id_docente=?",str(b,"nombre"),str(b,"apellido"),str(b,"cedula"),strOr(b,"estado","Activo"),id); return ResponseEntity.ok(Map.of("ok",true)); }
        catch(Exception e){return error(e);} }

    @GetMapping("/students")
    public Object students(HttpSession s) {
        if (!live(s)) return DemoData.students();
        return safeList("""
          SELECT a.id_alumno,a.nombre,a.apellido,a.`cédula` cedula,a.estado,
                 CONCAT(c.curso,' ',c.`sección`) curso,c.id_curso
          FROM alumno a INNER JOIN curso c ON c.id_curso=a.curso_id_curso
          ORDER BY c.id_curso,a.apellido,a.nombre
        """, DemoData.students());
    }

    @PostMapping("/students")
    public ResponseEntity<?> addStudent(@RequestBody Map<String,Object>b,HttpSession s){
        if(!live(s)) return ResponseEntity.ok(Map.of("ok",true,"demo",true));
        String nombre=str(b,"nombre"),apellido=str(b,"apellido"),cedula=str(b,"cedula"),pass=str(b,"password"),estado=strOr(b,"estado","Activo"); int curso=intv(b,"cursoId");
        try(Connection c=db.open()){ c.setAutoCommit(false);
            try(PreparedStatement ps=c.prepareStatement("INSERT INTO usuario(nombre,apellido,cedula,`contraseña`,rol,estado) VALUES(?,?,?,?,'ALUMNO',?)")){ps.setString(1,nombre);ps.setString(2,apellido);ps.setString(3,cedula);ps.setString(4,pass);ps.setString(5,estado);ps.executeUpdate();}
            try(PreparedStatement ps=c.prepareStatement("INSERT INTO alumno(nombre,apellido,`cédula`,estado,curso_id_curso) VALUES(?,?,?,?,?)")){ps.setString(1,nombre);ps.setString(2,apellido);ps.setString(3,cedula);ps.setString(4,estado);ps.setInt(5,curso);ps.executeUpdate();}
            c.commit();return ResponseEntity.ok(Map.of("ok",true));
        }catch(Exception e){return error(e);} }

    @PutMapping("/students/{id}")
    public ResponseEntity<?> editStudent(@PathVariable int id,@RequestBody Map<String,Object>b,HttpSession s){
        if(!live(s))return ResponseEntity.ok(Map.of("ok",true,"demo",true));
        try{db.update("UPDATE alumno SET nombre=?,apellido=?,`cédula`=?,estado=?,curso_id_curso=? WHERE id_alumno=?",str(b,"nombre"),str(b,"apellido"),str(b,"cedula"),strOr(b,"estado","Activo"),intv(b,"cursoId"),id);return ResponseEntity.ok(Map.of("ok",true));}catch(Exception e){return error(e);} }

    @GetMapping("/subjects")
    public Object subjects(HttpSession s){
        if(!live(s))return DemoData.subjects();
        SessionUser u=user(s); String where=""; List<Object> p=new ArrayList<>();
        if(u!=null && u.role().equals("DOCENTE")){where=" WHERE m.docente_id_docente=? ";p.add(u.teacherId());}
        if(u!=null && u.role().equals("ALUMNO")){where=" WHERE c.id_curso=(SELECT curso_id_curso FROM alumno WHERE id_alumno=?) ";p.add(u.studentId());}
        String sql="""
          SELECT MIN(m.id_materia) id_materia,m.nombre,
                 COALESCE(CONCAT(d.nombre,' ',d.apellido),'Sin asignar') docente,m.horas_catedras horas,
                 GROUP_CONCAT(CONCAT(c.curso,' ',c.`sección`) ORDER BY c.id_curso SEPARATOR ', ') cursos
          FROM materia m INNER JOIN curso c ON c.id_curso=m.curso_id_curso LEFT JOIN docente d ON d.id_docente=m.docente_id_docente
        """+where+" GROUP BY m.nombre,m.docente_id_docente,d.nombre,d.apellido,m.horas_catedras ORDER BY m.nombre";
        try{return db.query(sql,p.toArray());}catch(Exception e){return DemoData.subjects();}
    }

    @PostMapping("/subjects")
    public ResponseEntity<?> addSubject(@RequestBody Map<String,Object>b,HttpSession s){
        if(!live(s))return ResponseEntity.ok(Map.of("ok",true,"demo",true));
        try(Connection c=db.open()){ c.setAutoCommit(false); int docente=intv(b,"docenteId"),curso=intv(b,"cursoId"),horas=intv(b,"horas");
            int mid; try(PreparedStatement ps=c.prepareStatement("INSERT INTO materia(nombre,curso_id_curso,docente_id_docente,horas_catedras) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){ps.setString(1,str(b,"nombre"));ps.setInt(2,curso);if(docente>0)ps.setInt(3,docente);else ps.setNull(3,Types.INTEGER);ps.setInt(4,horas);ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){rs.next();mid=rs.getInt(1);}}
            if(docente>0)try(PreparedStatement ps=c.prepareStatement("INSERT IGNORE INTO docente_materia(docente_id_docente,materia_id_materia,curso_id_curso) VALUES(?,?,?)")){ps.setInt(1,docente);ps.setInt(2,mid);ps.setInt(3,curso);ps.executeUpdate();}
            c.commit();return ResponseEntity.ok(Map.of("ok",true));
        }catch(Exception e){return error(e);} }

    @GetMapping("/tasks")
    public Object tasks(HttpSession s){
        if(!live(s))return DemoData.tasks();
        SessionUser u=user(s); String where=" WHERE t.fecha_publicacion<=NOW() "; List<Object>p=new ArrayList<>();
        if(u!=null&&u.role().equals("DOCENTE")){where+=" AND t.docente_materia_docente_id_docente=? ";p.add(u.teacherId());}
        if(u!=null&&u.role().equals("ALUMNO")){where+=" AND c.id_curso=(SELECT curso_id_curso FROM alumno WHERE id_alumno=?) ";p.add(u.studentId());}
        String sql="""
          SELECT t.id_tarea,t.`título` titulo,t.`descripción` descripcion,t.`fecha_límite` fecha_limite,t.`puntaje_máximo` puntaje_maximo,
                 m.nombre materia,CONCAT(c.curso,' ',c.`sección`) curso,t.archivo_nombre,
                 CASE WHEN t.`fecha_límite`<CURDATE() THEN 'Vencida' ELSE 'Pendiente' END estado
          FROM tarea t INNER JOIN materia m ON t.docente_materia_materia_id_materia=m.id_materia INNER JOIN curso c ON c.id_curso=m.curso_id_curso
        """+where+" ORDER BY t.`fecha_límite` ASC,t.id_tarea DESC";
        try{return db.query(sql,p.toArray());}catch(Exception e){return DemoData.tasks();}
    }

    @PostMapping(value="/tasks", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addTask(@RequestParam String titulo,@RequestParam(required=false,defaultValue="")String descripcion,
        @RequestParam int puntaje,@RequestParam String fechaLimite,@RequestParam int materiaId,
        @RequestParam(required=false)String fechaPublicacion,@RequestPart(required=false) MultipartFile archivo,HttpSession s){
        if(!live(s))return ResponseEntity.ok(Map.of("ok",true,"demo",true)); SessionUser u=user(s); if(u==null||u.teacherId()<=0)return ResponseEntity.status(403).body(Map.of("ok",false,"message","Tu usuario no tiene un docente asociado."));
        try{ byte[] bytes=archivo!=null&&!archivo.isEmpty()?archivo.getBytes():null;String name=archivo!=null&&!archivo.isEmpty()?archivo.getOriginalFilename():null;
            LocalDateTime pub=(fechaPublicacion==null||fechaPublicacion.isBlank())?LocalDateTime.now():LocalDateTime.parse(fechaPublicacion);
            db.insert("INSERT INTO tarea(`título`,`descripción`,`puntaje_máximo`,`fecha_creación`,`fecha_publicacion`,`fecha_límite`,docente_materia_docente_id_docente,docente_materia_materia_id_materia,archivo,archivo_nombre,archivo_blob) VALUES(?,?,?,CURDATE(),?,?,?,?,NULL,?,?)",
                    titulo,descripcion,puntaje,Timestamp.valueOf(pub),java.sql.Date.valueOf(fechaLimite),u.teacherId(),materiaId,name,bytes);
            return ResponseEntity.ok(Map.of("ok",true));
        }catch(Exception e){return error(e);} }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable int id,HttpSession s){ if(!live(s))return ResponseEntity.ok(Map.of("ok",true,"demo",true));
        try(Connection c=db.open()){c.setAutoCommit(false);try(PreparedStatement p=c.prepareStatement("DELETE FROM entrega WHERE tarea_id_tarea=?")){p.setInt(1,id);p.executeUpdate();}try(PreparedStatement p=c.prepareStatement("DELETE FROM tarea WHERE id_tarea=?")){p.setInt(1,id);p.executeUpdate();}c.commit();return ResponseEntity.ok(Map.of("ok",true));}catch(Exception e){return error(e);} }

    @GetMapping("/submissions")
    public Object submissions(@RequestParam(required=false)Integer tareaId,HttpSession s){
        if(!live(s))return DemoData.submissions();
        try{
            if(tareaId!=null){ return db.query("""
              SELECT a.id_alumno,CONCAT(a.nombre,' ',a.apellido) alumno,e.id_entrega,COALESCE(e.estado_entrega,'Pendiente') estado,
                     COALESCE(DATE_FORMAT(e.fecha_entrega,'%Y-%m-%d'),'—') fecha,e.puntaje,t.`puntaje_máximo` maximo,e.`observación` observacion,e.archivo_nombre
              FROM tarea t INNER JOIN materia m ON t.docente_materia_materia_id_materia=m.id_materia INNER JOIN alumno a ON a.curso_id_curso=m.curso_id_curso
              LEFT JOIN entrega e ON e.tarea_id_tarea=t.id_tarea AND e.alumno_id_alumno=a.id_alumno WHERE t.id_tarea=? ORDER BY a.apellido,a.nombre
            """,tareaId); }
            SessionUser u=user(s); if(u!=null&&u.role().equals("ALUMNO"))return db.query("""
              SELECT e.id_entrega,t.`título` tarea,m.nombre materia,e.estado_entrega estado,DATE_FORMAT(e.fecha_entrega,'%Y-%m-%d') fecha,e.puntaje,t.`puntaje_máximo` maximo
              FROM entrega e INNER JOIN tarea t ON e.tarea_id_tarea=t.id_tarea INNER JOIN materia m ON t.docente_materia_materia_id_materia=m.id_materia WHERE e.alumno_id_alumno=? ORDER BY e.fecha_entrega DESC
            """,u.studentId());
            return db.query("""
              SELECT e.id_entrega,CONCAT(a.nombre,' ',a.apellido) alumno,t.`título` tarea,m.nombre materia,e.estado_entrega estado,DATE_FORMAT(e.fecha_entrega,'%Y-%m-%d') fecha,e.puntaje,t.`puntaje_máximo` maximo
              FROM entrega e INNER JOIN alumno a ON e.alumno_id_alumno=a.id_alumno INNER JOIN tarea t ON e.tarea_id_tarea=t.id_tarea INNER JOIN materia m ON t.docente_materia_materia_id_materia=m.id_materia ORDER BY e.fecha_entrega DESC LIMIT 200
            """);
        }catch(Exception e){return DemoData.submissions();}
    }

    @PutMapping("/submissions/{id}/grade")
    public ResponseEntity<?> grade(@PathVariable int id,@RequestBody Map<String,Object>b,HttpSession s){ if(!live(s))return ResponseEntity.ok(Map.of("ok",true,"demo",true));
        try{db.update("UPDATE entrega SET puntaje=?,`observación`=? WHERE id_entrega=?",intv(b,"puntaje"),strOr(b,"observacion",""),id);return ResponseEntity.ok(Map.of("ok",true));}catch(Exception e){return error(e);} }

    @PostMapping(value="/student/submissions",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submit(@RequestParam int tareaId,@RequestPart(required=false)MultipartFile archivo,HttpSession s){ SessionUser u=user(s);if(u==null||u.studentId()<=0)return ResponseEntity.status(403).body(Map.of("ok",false));if(!live(s))return ResponseEntity.ok(Map.of("ok",true,"demo",true));
        try{byte[]bytes=archivo!=null&&!archivo.isEmpty()?archivo.getBytes():null;String name=archivo!=null&&!archivo.isEmpty()?archivo.getOriginalFilename():null;String estado="Entregada";
            List<Map<String,Object>> t=db.query("SELECT `fecha_límite` limite FROM tarea WHERE id_tarea=?",tareaId);if(!t.isEmpty()&&LocalDate.parse(String.valueOf(t.get(0).get("limite"))).isBefore(LocalDate.now()))estado="Tardía";
            db.insert("INSERT INTO entrega(estado_entrega,fecha_entrega,archivo,archivo_nombre,archivo_blob,tarea_id_tarea,alumno_id_alumno) VALUES(?,NOW(),NULL,?,?,?,?)",estado,name,bytes,tareaId,u.studentId());return ResponseEntity.ok(Map.of("ok",true));
        }catch(Exception e){return error(e);} }

    @GetMapping("/position-catalog")
    public Object positionCatalog(HttpSession s){
        if(!live(s)) return List.of(DemoData.map("id_cargo",1,"nombre","Delegado"),DemoData.map("id_cargo",2,"nombre","Encargado de carpeta"),DemoData.map("id_cargo",3,"nombre","Encargado de proyector"),DemoData.map("id_cargo",4,"nombre","Tesorero"));
        return safeList("SELECT id_cargo,nombre FROM cargo ORDER BY nombre",List.of());
    }

    @PostMapping("/positions")
    public ResponseEntity<?> addPosition(@RequestBody Map<String,Object>b,HttpSession s){
        if(!live(s)) return ResponseEntity.ok(Map.of("ok",true,"demo",true));
        try{
            int alumno=intv(b,"alumnoId"),cargo=intv(b,"cargoId");
            String fecha=strOr(b,"fechaInicio",LocalDate.now().toString()),obs=strOr(b,"observacion","");
            db.insert("INSERT INTO `asignación_cargo`(cargo_id_cargo,estado,fecha_inicio,observacion,id_alumno) VALUES(?,'Activo',?,?,?)",cargo,java.sql.Date.valueOf(fecha),obs,alumno);
            return ResponseEntity.ok(Map.of("ok",true));
        }catch(Exception e){return error(e);}
    }

    @GetMapping("/positions")
    public Object positions(HttpSession s){if(!live(s))return DemoData.positions();return safeList("""
      SELECT ac.id_asignacion,CONCAT(a.nombre,' ',a.apellido) alumno,CONCAT(c.curso,' ',c.`sección`) curso,ca.nombre cargo,ac.estado,
             COALESCE(GROUP_CONCAT(DISTINCT m.nombre ORDER BY m.nombre SEPARATOR ', '),'Sin materias configuradas') materias,
             COALESCE(MAX(cm.puntos_aplicables),0) puntos_extra
      FROM `asignación_cargo` ac INNER JOIN alumno a ON a.id_alumno=ac.id_alumno INNER JOIN curso c ON c.id_curso=a.curso_id_curso INNER JOIN cargo ca ON ca.id_cargo=ac.cargo_id_cargo
      LEFT JOIN cargo_materia cm ON cm.cargo_id_cargo=ca.id_cargo LEFT JOIN materia m ON m.id_materia=cm.materia_id_materia AND m.curso_id_curso=a.curso_id_curso
      WHERE ac.estado='Activo' GROUP BY ac.id_asignacion,a.nombre,a.apellido,c.curso,c.`sección`,ca.nombre,ac.estado ORDER BY c.id_curso,a.apellido
    """,DemoData.positions());}

    @GetMapping("/suggestions")
    public Object suggestions(HttpSession s){if(!live(s))return DemoData.suggestions(); SessionUser u=user(s); try{
        if(u!=null&&u.role().equals("DOCENTE"))return db.query("""
          SELECT s.id_sugerencia,s.mensaje,s.fecha_envio,s.estado,m.nombre materia,CONCAT(a.nombre,' ',a.apellido) alumno
          FROM sugerencia s INNER JOIN materia m ON s.materia_id=m.id_materia INNER JOIN alumno a ON s.alumno_id=a.id_alumno WHERE m.docente_id_docente=? ORDER BY s.fecha_envio DESC
        """,u.teacherId());
        if(u!=null&&u.role().equals("ALUMNO"))return db.query("""
          SELECT s.id_sugerencia,s.mensaje,s.fecha_envio,s.estado,m.nombre materia FROM sugerencia s INNER JOIN materia m ON s.materia_id=m.id_materia WHERE s.alumno_id=? ORDER BY s.fecha_envio DESC
        """,u.studentId());
        return db.query("SELECT s.id_sugerencia,s.mensaje,s.fecha_envio,s.estado,m.nombre materia,CONCAT(a.nombre,' ',a.apellido) alumno FROM sugerencia s INNER JOIN materia m ON s.materia_id=m.id_materia INNER JOIN alumno a ON s.alumno_id=a.id_alumno ORDER BY s.fecha_envio DESC LIMIT 200");
    }catch(Exception e){return DemoData.suggestions();}}

    @PostMapping("/suggestions")
    public ResponseEntity<?> addSuggestion(@RequestBody Map<String,Object>b,HttpSession s){SessionUser u=user(s);if(u==null||u.studentId()<=0)return ResponseEntity.status(403).body(Map.of("ok",false));if(!live(s))return ResponseEntity.ok(Map.of("ok",true,"demo",true));try{db.insert("INSERT INTO sugerencia(alumno_id,materia_id,mensaje,fecha_envio,estado) VALUES(?,?,?,NOW(),'Nueva')",u.studentId(),intv(b,"materiaId"),str(b,"mensaje"));return ResponseEntity.ok(Map.of("ok",true));}catch(Exception e){return error(e);} }

    @PatchMapping("/suggestions/{id}")
    public ResponseEntity<?> readSuggestion(@PathVariable int id,HttpSession s){if(!live(s))return ResponseEntity.ok(Map.of("ok",true,"demo",true));try{db.update("UPDATE sugerencia SET estado='Leída' WHERE id_sugerencia=?",id);return ResponseEntity.ok(Map.of("ok",true));}catch(Exception e){return error(e);} }

    @GetMapping("/grades")
    public Object grades(HttpSession s){SessionUser u=user(s);if(!live(s))return DemoData.grades();if(u==null||u.studentId()<=0)return List.of();try{return db.query("""
      SELECT m.nombre materia,t.`título` tarea,e.puntaje,t.`puntaje_máximo` maximo,ROUND(e.puntaje/t.`puntaje_máximo`*100,1) porcentaje,
             CASE WHEN e.puntaje/t.`puntaje_máximo`*100>=70 THEN 'Logrado' ELSE 'No logrado' END estado
      FROM entrega e INNER JOIN tarea t ON e.tarea_id_tarea=t.id_tarea INNER JOIN materia m ON t.docente_materia_materia_id_materia=m.id_materia
      WHERE e.alumno_id_alumno=? AND e.puntaje IS NOT NULL ORDER BY m.nombre,t.`fecha_límite` DESC
    """,u.studentId());}catch(Exception e){return DemoData.grades();}}

    @GetMapping("/streak")
    public Object streak(HttpSession s){if(!live(s))return DemoData.streak();SessionUser u=user(s);try{
        int course=0;if(u!=null&&u.studentId()>0){List<Map<String,Object>>r=db.query("SELECT curso_id_curso id FROM alumno WHERE id_alumno=?",u.studentId());if(!r.isEmpty())course=((Number)r.get(0).get("id")).intValue();}
        if(course>0)return db.query("""
          SELECT ROW_NUMBER() OVER(ORDER BY COALESCE(r.racha_actual,0) DESC,a.apellido) posicion,CONCAT(a.nombre,' ',a.apellido) alumno,COALESCE(r.racha_actual,0) racha
          FROM alumno a LEFT JOIN racha_alumno r ON r.alumno_id=a.id_alumno WHERE a.curso_id_curso=? ORDER BY racha DESC,a.apellido
        """,course);
        return db.query("SELECT ROW_NUMBER() OVER(ORDER BY COALESCE(r.racha_actual,0) DESC) posicion,CONCAT(a.nombre,' ',a.apellido) alumno,COALESCE(r.racha_actual,0) racha FROM alumno a LEFT JOIN racha_alumno r ON r.alumno_id=a.id_alumno ORDER BY racha DESC LIMIT 20");
    }catch(Exception e){return DemoData.streak();}}

    private Map<String,Object> one(String sql,Object...p)throws SQLException{List<Map<String,Object>>l=db.query(sql,p);return l.isEmpty()?Map.of():l.get(0);}
    private Object safeList(String sql,List<Map<String,Object>>fallback,Object...p){try{return db.query(sql,p);}catch(Exception e){return fallback;}}
    private static String str(Map<String,Object>b,String k){return Objects.toString(b.get(k),"").trim();}
    private static String strOr(Map<String,Object>b,String k,String d){String x=str(b,k);return x.isBlank()?d:x;}
    private static int intv(Map<String,Object>b,String k){Object v=b.get(k);if(v instanceof Number n)return n.intValue();try{return Integer.parseInt(Objects.toString(v,"0"));}catch(Exception e){return 0;}}
    private static ResponseEntity<?> error(Exception e){return ResponseEntity.badRequest().body(Map.of("ok",false,"message",e.getMessage()==null?"No se pudo completar la operación.":e.getMessage()));}
}
