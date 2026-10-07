package com.gema.web;

import java.time.LocalDate;
import java.util.*;

public final class DemoData {
    private DemoData() {}

    public static Map<String,Object> dashboard(String role) {
        if ("ALUMNO".equals(role)) return map("pending",4,"submitted",7,"graded",5,"streak",12);
        if ("DOCENTE".equals(role)) return map("subjects",5,"tasks",12,"submissions",34,"pendingGrades",8);
        return map("students",126,"teachers",18,"tasks",47,"submissions",318);
    }

    public static List<Map<String,Object>> courses() { return List.of(
        map("id_curso",1,"curso","1°","seccion","A","año",2026), map("id_curso",2,"curso","1°","seccion","B","año",2026),
        map("id_curso",3,"curso","2°","seccion","A","año",2026), map("id_curso",4,"curso","2°","seccion","B","año",2026),
        map("id_curso",5,"curso","3°","seccion","A","año",2026), map("id_curso",6,"curso","3°","seccion","B","año",2026)); }

    public static List<Map<String,Object>> teachers() { return List.of(
        map("id_docente",1,"nombre","Graciela","apellido","López","cedula","4.825.410","estado","Activo","materias","Algorítmica"),
        map("id_docente",2,"nombre","María","apellido","Gómez","cedula","3.912.778","estado","Activo","materias","Matemática, Matemática Aplicada"),
        map("id_docente",3,"nombre","Carlos","apellido","Benítez","cedula","5.174.002","estado","Activo","materias","Laboratorio Java, Laboratorio Android")); }

    public static List<Map<String,Object>> students() { return List.of(
        map("id_alumno",1,"nombre","Sofía","apellido","Martínez","cedula","7.101.245","estado","Activo","curso","3° B"),
        map("id_alumno",2,"nombre","Mateo","apellido","Rojas","cedula","7.204.183","estado","Activo","curso","3° B"),
        map("id_alumno",3,"nombre","Valentina","apellido","Acosta","cedula","7.330.902","estado","Activo","curso","3° A"),
        map("id_alumno",4,"nombre","Thiago","apellido","Vera","cedula","7.118.704","estado","Activo","curso","2° B")); }

    public static List<Map<String,Object>> subjects() { return List.of(
        map("id_materia",1,"nombre","Algorítmica","docente","Graciela López","horas",4,"cursos","1° A, 1° B, 2° A, 2° B, 3° A, 3° B"),
        map("id_materia",2,"nombre","Laboratorio Java","docente","Carlos Benítez","horas",4,"cursos","3° A, 3° B"),
        map("id_materia",3,"nombre","Matemática","docente","María Gómez","horas",4,"cursos","1° A, 1° B, 2° A, 2° B, 3° A, 3° B"),
        map("id_materia",4,"nombre","Redes","docente","Carlos Benítez","horas",3,"cursos","3° A, 3° B")); }

    public static List<Map<String,Object>> tasks() {
        LocalDate now = LocalDate.now();
        return List.of(
            map("id_tarea",1,"titulo","Proyecto final Java","descripcion","Entrega del proyecto con documentación.","materia","Laboratorio Java","curso","3° B","fecha_limite",now.plusDays(3).toString(),"puntaje_maximo",20,"estado","Pendiente"),
            map("id_tarea",2,"titulo","Ejercicios de derivadas","descripcion","Resolver la guía de práctica.","materia","Matemática","curso","3° B","fecha_limite",now.plusDays(1).toString(),"puntaje_maximo",15,"estado","Pendiente"),
            map("id_tarea",3,"titulo","Modelo TCP/IP","descripcion","Resumen y esquema.","materia","Redes","curso","3° B","fecha_limite",now.minusDays(2).toString(),"puntaje_maximo",10,"estado","Entregada"));
    }

    public static List<Map<String,Object>> submissions() { return List.of(
        map("id_entrega",1,"alumno","Sofía Martínez","estado","Entregada","fecha","2026-10-06","puntaje",18,"maximo",20),
        map("id_entrega",2,"alumno","Mateo Rojas","estado","Tardía","fecha","2026-10-07","puntaje",15,"maximo",20),
        map("id_entrega",3,"alumno","Valentina Acosta","estado","Pendiente","fecha","—","puntaje",null,"maximo",20)); }

    public static List<Map<String,Object>> positions() { return List.of(
        map("id_asignacion",1,"alumno","Sofía Martínez","curso","3° B","cargo","Delegado","materias","Todas","puntos_extra",2,"estado","Activo"),
        map("id_asignacion",2,"alumno","Mateo Rojas","curso","3° B","cargo","Encargado de proyector","materias","Laboratorio Java","puntos_extra",1,"estado","Activo")); }

    public static List<Map<String,Object>> suggestions() { return List.of(
        map("id_sugerencia",1,"alumno","Sofía Martínez","materia","Laboratorio Java","mensaje","¿Podríamos tener un ejemplo extra de conexión a base de datos?","fecha_envio","2026-10-07T08:30","estado","Nueva"),
        map("id_sugerencia",2,"alumno","Mateo Rojas","materia","Redes","mensaje","Sería útil subir una guía de repaso para el parcial.","fecha_envio","2026-10-06T15:10","estado","Leída")); }

    public static List<Map<String,Object>> grades() { return List.of(
        map("materia","Laboratorio Java","tarea","Proyecto interfaz","puntaje",18,"maximo",20,"porcentaje",90,"estado","Logrado"),
        map("materia","Matemática","tarea","Límites y continuidad","puntaje",13,"maximo",15,"porcentaje",86.7,"estado","Logrado"),
        map("materia","Redes","tarea","Topologías","puntaje",7,"maximo",10,"porcentaje",70,"estado","Logrado")); }

    public static List<Map<String,Object>> streak() { return List.of(
        map("posicion",1,"alumno","Sofía Martínez","racha",18),
        map("posicion",2,"alumno","Mateo Rojas","racha",14),
        map("posicion",3,"alumno","Valentina Acosta","racha",12),
        map("posicion",4,"alumno","Thiago Vera","racha",9)); }

    @SuppressWarnings("unchecked")
    public static Map<String,Object> map(Object... kv) {
        Map<String,Object> m = new LinkedHashMap<>();
        for (int i=0;i<kv.length;i+=2) m.put(String.valueOf(kv[i]), kv[i+1]);
        return m;
    }
}
