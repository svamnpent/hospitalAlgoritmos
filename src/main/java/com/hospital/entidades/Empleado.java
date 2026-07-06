package com.hospital.entidades;

import java.sql.Date;
import java.sql.Time;

public class Empleado extends Persona {
    private int idRol;
    private String username;
    private String password;
    private double sueldoBase;
    private Date fechaContratacion;
    private String estadoContrato;
    private String diaDescanso;
    private Time horaEntrada;
    private Time horaSalida;

    public Empleado() { super(); }

    public int getIdRol() { return idRol; }
    public void setIdRol(int idRol) { this.idRol = idRol; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public double getSueldoBase() { return sueldoBase; }
    public void setSueldoBase(double sueldoBase) { this.sueldoBase = sueldoBase; }
    public Date getFechaContratacion() { return fechaContratacion; }
    public void setFechaContratacion(Date fechaContratacion) { this.fechaContratacion = fechaContratacion; }
    public String getEstadoContrato() { return estadoContrato; }
    public void setEstadoContrato(String estadoContrato) { this.estadoContrato = estadoContrato; }
    public String getDiaDescanso() { return diaDescanso; }
    public void setDiaDescanso(String diaDescanso) { this.diaDescanso = diaDescanso; }
    public Time getHoraEntrada() { return horaEntrada; }
    public void setHoraEntrada(Time horaEntrada) { this.horaEntrada = horaEntrada; }
    public Time getHoraSalida() { return horaSalida; }
    public void setHoraSalida(Time horaSalida) { this.horaSalida = horaSalida; }

    // Nombre del rol para mostrar en vistas
    public String getNombreRol() {
        return switch (idRol) {
            case 1 -> "ADMIN";
            case 2 -> "RECEPCIONISTA";
            case 3 -> "MEDICO";
            case 4 -> "CONTADOR";
            case 5 -> "RRHH";
            default -> "DESCONOCIDO";
        };
    }
}
