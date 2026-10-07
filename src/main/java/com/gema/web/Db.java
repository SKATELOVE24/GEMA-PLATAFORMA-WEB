package com.gema.web;

import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

@Component
public class Db {
    private final Properties cfg = new Properties();

    public Db() {
        Path p = Path.of("config", "gema.properties");
        if (Files.exists(p)) {
            try (InputStream in = Files.newInputStream(p)) { cfg.load(in); }
            catch (Exception ignored) { }
        }
    }

    private String value(String env, String key, String def) {
        String e = System.getenv(env);
        if (e != null && !e.isBlank()) return e.trim();
        String p = cfg.getProperty(key);
        return (p == null || p.isBlank()) ? def : p.trim();
    }

    public boolean configured() {
        return !value("GEMA_DB_HOST", "db.host", "").isBlank()
                && !value("GEMA_DB_NAME", "db.name", "").isBlank()
                && !value("GEMA_DB_USER", "db.user", "").isBlank();
    }

    public Connection open() throws SQLException {
        if (!configured()) throw new SQLException("Base de datos no configurada");
        String host = value("GEMA_DB_HOST", "db.host", "");
        String db = value("GEMA_DB_NAME", "db.name", "");
        String user = value("GEMA_DB_USER", "db.user", "");
        String pass = value("GEMA_DB_PASSWORD", "db.password", "");
        String url = "jdbc:mysql://" + host + "/" + db
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Asuncion&connectTimeout=8000&socketTimeout=15000";
        return DriverManager.getConnection(url, user, pass);
    }

    public Map<String,Object> status() {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("configured", configured());
        if (!configured()) {
            m.put("online", false);
            m.put("message", "Modo demo: configurá config/gema.properties para usar la base real.");
            return m;
        }
        try (Connection c = open()) {
            m.put("online", c.isValid(3));
            m.put("message", "Conectado a GEMA DB");
        } catch (Exception e) {
            m.put("online", false);
            m.put("message", "La web funciona en modo demo porque la base no respondió.");
        }
        return m;
    }

    public List<Map<String,Object>> query(String sql, Object... args) throws SQLException {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String,Object>> out = new ArrayList<>();
                ResultSetMetaData md = rs.getMetaData();
                while (rs.next()) {
                    Map<String,Object> row = new LinkedHashMap<>();
                    for (int i=1;i<=md.getColumnCount();i++) {
                        Object v = rs.getObject(i);
                        if (v instanceof Timestamp t) v = t.toLocalDateTime().toString();
                        else if (v instanceof java.sql.Date d) v = d.toLocalDate().toString();
                        row.put(md.getColumnLabel(i), v);
                    }
                    out.add(row);
                }
                return out;
            }
        }
    }

    public int update(String sql, Object... args) throws SQLException {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, args);
            return ps.executeUpdate();
        }
    }

    public int insert(String sql, Object... args) throws SQLException {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, args);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    static void bind(PreparedStatement ps, Object... args) throws SQLException {
        for (int i=0;i<args.length;i++) {
            Object v = args[i];
            if (v == null) ps.setObject(i+1, null);
            else if (v instanceof Integer n) ps.setInt(i+1, n);
            else if (v instanceof Long n) ps.setLong(i+1, n);
            else if (v instanceof Double n) ps.setDouble(i+1, n);
            else if (v instanceof byte[] b) ps.setBytes(i+1, b);
            else ps.setObject(i+1, v);
        }
    }
}
