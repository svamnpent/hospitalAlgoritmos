package com.hospital.entidades;


public class ReporteCitas {
    private long totalCitas;
    private long citasAtendidas;
    private long citasPendientes;
    private long citasCanceladas;
    private long citasHoy;
    private long citasEstaSemana;
    private long citasEsteMes;
    private long pacientesRegistrados;
    private long medicosActivos;
    private long empleadosActivos;

    public long getTotalCitas() {
        return totalCitas;
    }

    public void setTotalCitas(long totalCitas) {
        this.totalCitas = totalCitas;
    }

    public long getCitasAtendidas() {
        return citasAtendidas;
    }

    public void setCitasAtendidas(long citasAtendidas) {
        this.citasAtendidas = citasAtendidas;
    }

    public long getCitasPendientes() {
        return citasPendientes;
    }

    public void setCitasPendientes(long citasPendientes) {
        this.citasPendientes = citasPendientes;
    }

    public long getCitasCanceladas() {
        return citasCanceladas;
    }

    public void setCitasCanceladas(long citasCanceladas) {
        this.citasCanceladas = citasCanceladas;
    }

    public long getCitasHoy() {
        return citasHoy;
    }

    public void setCitasHoy(long citasHoy) {
        this.citasHoy = citasHoy;
    }

    public long getCitasEstaSemana() {
        return citasEstaSemana;
    }

    public void setCitasEstaSemana(long citasEstaSemana) {
        this.citasEstaSemana = citasEstaSemana;
    }

    public long getCitasEsteMes() {
        return citasEsteMes;
    }

    public void setCitasEsteMes(long citasEsteMes) {
        this.citasEsteMes = citasEsteMes;
    }

    public long getPacientesRegistrados() {
        return pacientesRegistrados;
    }

    public void setPacientesRegistrados(long pacientesRegistrados) {
        this.pacientesRegistrados = pacientesRegistrados;
    }

    public long getMedicosActivos() {
        return medicosActivos;
    }

    public void setMedicosActivos(long medicosActivos) {
        this.medicosActivos = medicosActivos;
    }

    public long getEmpleadosActivos() {
        return empleadosActivos;
    }

    public void setEmpleadosActivos(long empleadosActivos) {
        this.empleadosActivos = empleadosActivos;
    }
}
