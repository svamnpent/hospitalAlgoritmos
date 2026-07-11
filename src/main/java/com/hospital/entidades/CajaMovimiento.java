package com.hospital.entidades;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class CajaMovimiento {
    private int idMovimiento;
    private BigDecimal monto;
    private String tipo;
    private String categoria;
    private String descripcion;
    private Timestamp fecha;
    private int idContador;
    private String nombreContador;
    private boolean revertido;
    private int revertidoPor;

    public int getIdMovimiento() { return idMovimiento; }
    public void setIdMovimiento(int idMovimiento) { this.idMovimiento = idMovimiento; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Timestamp getFecha() { return fecha; }
    public void setFecha(Timestamp fecha) { this.fecha = fecha; }
    public int getIdContador() { return idContador; }
    public void setIdContador(int idContador) { this.idContador = idContador; }
    public String getNombreContador() { return nombreContador; }
    public void setNombreContador(String nombreContador) { this.nombreContador = nombreContador; }
    public boolean isRevertido() { return revertido; }
    public void setRevertido(boolean revertido) { this.revertido = revertido; }
    public int getRevertidoPor() { return revertidoPor; }
    public void setRevertidoPor(int revertidoPor) { this.revertidoPor = revertidoPor; }
}

