package com.hospital.entidades;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

public class Cita {
    private int idCita;
    private int idPaciente;
    private int idMedico;
    private int idServicio;
    private Date fechaCita;
    private Timestamp fechaCitaCom;
    private String estado;
    private Time horaCita;
    private int duracionServicioMinutos;

    // Campos auxiliares para JOINs (presentación)
    private String nombrePaciente;
    private String nombreServicio;
    private String nombreMedico;
    private double costo;


    public Cita() {}


    // Getters y Setters
    public String getNombreMedico() { return nombreMedico; }
    public void setNombreMedico(String nombreMedico) { this.nombreMedico = nombreMedico; }
    public double getCosto() { return costo; }
    public void setCosto(double costo) { this.costo = costo; }


    public int getIdCita() { return idCita; }
    public void setIdCita(int idCita) { this.idCita = idCita; }
    public int getIdPaciente() { return idPaciente; }
    public void setIdPaciente(int idPaciente) { this.idPaciente = idPaciente; }
    public int getIdMedico() { return idMedico; }
    public void setIdMedico(int idMedico) { this.idMedico = idMedico; }
    public int getIdServicio() { return idServicio; }
    public void setIdServicio(int idServicio) { this.idServicio = idServicio; }
    public Date getFechaCita() { return fechaCita; }
    public void setFechaCita(Date fechaCita) { this.fechaCita = fechaCita; }
    public Timestamp getFechaCitaCom() { return fechaCitaCom; }
    public void setFechaCitaCom(Timestamp fechaCitaCom) { this.fechaCitaCom = fechaCitaCom; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Time getHoraCita() { return horaCita; }
    public void setHoraCita(Time horaCita) { this.horaCita = horaCita; }
    public int getDuracionServicioMinutos() { return duracionServicioMinutos; }
    public void setDuracionServicioMinutos(int duracionServicioMinutos) { this.duracionServicioMinutos = duracionServicioMinutos; }
    public String getNombrePaciente() { return nombrePaciente; }
    public void setNombrePaciente(String nombrePaciente) { this.nombrePaciente = nombrePaciente; }
    public String getNombreServicio() { return nombreServicio; }
    public void setNombreServicio(String nombreServicio) { this.nombreServicio = nombreServicio; }
}
