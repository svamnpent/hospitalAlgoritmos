// ReportePersonal.java
package com.hospital.entidades;

public class ReportePersonal {
    private long totalEmpleados;
    private long activos;
    private long inactivos;
    private long doctores;
    private long recepcionistas;
    private long admins;
    private long contadores;
    private long rrhh;
    private double totalPlanilla;

    // Getters y Setters (generar con IDE Alt+Insert)
    public long getTotalEmpleados() { return totalEmpleados; }
    public void setTotalEmpleados(long totalEmpleados) { this.totalEmpleados = totalEmpleados; }
    public long getActivos() { return activos; }
    public void setActivos(long activos) { this.activos = activos; }
    public long getInactivos() { return inactivos; }
    public void setInactivos(long inactivos) { this.inactivos = inactivos; }
    public long getDoctores() { return doctores; }
    public void setDoctores(long doctores) { this.doctores = doctores; }
    public long getRecepcionistas() { return recepcionistas; }
    public void setRecepcionistas(long recepcionistas) { this.recepcionistas = recepcionistas; }
    public long getAdmins() { return admins; }
    public void setAdmins(long admins) { this.admins = admins; }
    public long getContadores() { return contadores; }
    public void setContadores(long contadores) { this.contadores = contadores; }
    public long getRrhh() { return rrhh; }
    public void setRrhh(long rrhh) { this.rrhh = rrhh; }
    public double getTotalPlanilla() { return totalPlanilla; }
    public void setTotalPlanilla(double totalPlanilla) { this.totalPlanilla = totalPlanilla; }
}