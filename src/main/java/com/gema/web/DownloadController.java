package com.gema.web;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@RestController
@RequestMapping("/api/files")
public class DownloadController {
    private final Db db;
    public DownloadController(Db db){this.db=db;}

    @GetMapping("/task/{id}")
    public ResponseEntity<byte[]> task(@PathVariable int id){
        try(Connection c=db.open(); PreparedStatement ps=c.prepareStatement("SELECT archivo_nombre,archivo_blob FROM tarea WHERE id_tarea=?")){
            ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){if(!rs.next()||rs.getBytes("archivo_blob")==null)return ResponseEntity.notFound().build();
                String name=rs.getString("archivo_nombre");return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+safe(name)+"\"").contentType(MediaType.APPLICATION_OCTET_STREAM).body(rs.getBytes("archivo_blob"));}
        }catch(Exception e){return ResponseEntity.notFound().build();}
    }
    @GetMapping("/submission/{id}")
    public ResponseEntity<byte[]> submission(@PathVariable int id){
        try(Connection c=db.open(); PreparedStatement ps=c.prepareStatement("SELECT archivo_nombre,archivo_blob FROM entrega WHERE id_entrega=?")){
            ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){if(!rs.next()||rs.getBytes("archivo_blob")==null)return ResponseEntity.notFound().build();
                String name=rs.getString("archivo_nombre");return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+safe(name)+"\"").contentType(MediaType.APPLICATION_OCTET_STREAM).body(rs.getBytes("archivo_blob"));}
        }catch(Exception e){return ResponseEntity.notFound().build();}
    }
    private String safe(String s){if(s==null||s.isBlank())return "archivo";return s.replace("\"","").replace("\r","").replace("\n","");}
}
