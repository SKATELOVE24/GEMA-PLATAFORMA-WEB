package com.gema.web;

import java.io.Serializable;

public record SessionUser(String role, String name, String cedula, int teacherId, int studentId, boolean demo) implements Serializable {}
