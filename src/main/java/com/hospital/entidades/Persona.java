package com.hospital.entidades;

import java.sql.Date;

public abstract class Persona {
    protected int idPersona;
    protected String dni;
    protected String nombre;
    protected String apellido;
    protected String telefono;
    protected Date fechaNacimiento;
    protected String direccion;
    protected String genero;

    public Persona() {}

    public int getIdPersona() { return idPersona; }
    public void setIdPersona(int idPersona) { this.idPersona = idPersona; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public Date getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(Date fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }
}
