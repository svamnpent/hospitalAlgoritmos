package com.hospital.entidades;

import java.sql.Time;

public class Medico extends Persona {
    private int idEspecialidad;
    private String nombreEspecialidad;
    private String diaDescanso;
    private Time horaInicio;
    private Time horaFin;

    public Medico() { super(); }

    public int getIdEspecialidad() { return idEspecialidad; }
    public void setIdEspecialidad(int idEspecialidad) { this.idEspecialidad = idEspecialidad; }
    public String getNombreEspecialidad() { return nombreEspecialidad; }
    public void setNombreEspecialidad(String nombreEspecialidad) { this.nombreEspecialidad = nombreEspecialidad; }
    public String getDiaDescanso() { return diaDescanso; }
    public void setDiaDescanso(String diaDescanso) { this.diaDescanso = diaDescanso; }
    public Time getHoraInicio() { return horaInicio; }
    public void setHoraInicio(Time horaInicio) { this.horaInicio = horaInicio; }
    public Time getHoraFin() { return horaFin; }
    public void setHoraFin(Time horaFin) { this.horaFin = horaFin; }

    @Override
    public String toString() {
        return this.nombre + " " + this.apellido;
    }
}
