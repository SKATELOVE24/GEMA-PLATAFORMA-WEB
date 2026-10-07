package com.gema.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final Db db;
    public AuthController(Db db) { this.db = db; }

    @GetMapping("/status")
    public Map<String,Object> status(HttpSession session) {
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("database", db.status());
        out.put("session", session.getAttribute("user"));
        out.put("app", "GEMA Web");
        out.put("version", "1.0.0");
        return out;
    }

    @PostMapping("/login")
    public Map<String,Object> login(@RequestBody Map<String,Object> body, HttpSession session) throws Exception {
        String demoRole = Objects.toString(body.get("demoRole"), "").trim();
        if (!demoRole.isBlank()) {
            String role = normalizeRole(demoRole);
            SessionUser u = switch (role) {
                case "DOCENTE" -> new SessionUser("DOCENTE", "Docente Demo", "demo", 1, 0, true);
                case "ALUMNO" -> new SessionUser("ALUMNO", "Alumno Demo", "demo", 0, 1, true);
                default -> new SessionUser("COORDINADOR", "Coordinación Demo", "demo", 1, 0, true);
            };
            session.setAttribute("user", u);
            return Map.of("ok", true, "user", u, "mode", "demo");
        }

        String cedula = Objects.toString(body.get("cedula"), "").trim();
        String password = Objects.toString(body.get("password"), "");
        if (cedula.isBlank() || password.isBlank()) return Map.of("ok", false, "message", "Ingresá tu cédula y contraseña.");
        if (!db.configured()) return Map.of("ok", false, "message", "La base real todavía no está configurada. Podés entrar con los botones Demo.");

        try (Connection c = db.open();
             PreparedStatement ps = c.prepareStatement("SELECT id_usuario, rol, nombre, apellido FROM usuario WHERE TRIM(cedula)=? AND `contraseña`=? LIMIT 1")) {
            ps.setString(1, cedula); ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Map.of("ok", false, "message", "La cédula o contraseña son incorrectas.");
                String role = normalizeRole(rs.getString("rol"));
                String name = (Objects.toString(rs.getString("nombre"), "") + " " + Objects.toString(rs.getString("apellido"), "")).trim();
                int teacherId = 0, studentId = 0;
                if (role.equals("DOCENTE") || role.equals("COORDINADOR")) teacherId = lookupId(c, "docente", "id_docente", cedula);
                if (role.equals("ALUMNO")) studentId = lookupId(c, "alumno", "id_alumno", cedula);
                SessionUser u = new SessionUser(role, name.isBlank() ? role : name, cedula, teacherId, studentId, false);
                session.setAttribute("user", u);
                return Map.of("ok", true, "user", u, "mode", "live");
            }
        }
    }

    @PostMapping("/logout")
    public Map<String,Object> logout(HttpSession session) {
        session.invalidate();
        return Map.of("ok", true);
    }

    private int lookupId(Connection c, String table, String idCol, String cedula) {
        String sql = "SELECT " + idCol + " FROM " + table + " WHERE TRIM(`cédula`)=? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cedula);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        } catch (Exception e) { return 0; }
    }

    private static String normalizeRole(String r) {
        String x = Objects.toString(r, "COORDINADOR").trim().toUpperCase(Locale.ROOT)
                .replace('Á','A').replace('É','E').replace('Í','I').replace('Ó','O').replace('Ú','U').replace('Ñ','N');
        if (x.contains("ALUM")) return "ALUMNO";
        if (x.contains("DOCENTE") && !x.contains("COORD")) return "DOCENTE";
        return "COORDINADOR";
    }
}
