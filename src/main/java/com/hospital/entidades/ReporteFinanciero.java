// ReporteFinanciero.java
package com.hospital.entidades;

import java.math.BigDecimal;

public class ReporteFinanciero {
    private BigDecimal totalIngresos;
    private BigDecimal totalEgresos;
    private BigDecimal saldo;
    private BigDecimal ingresosPorCitas;
    private BigDecimal egresosPlanilla;
    private BigDecimal egresosInsumos;
    private long totalPagos;
    private long totalCitasPagadas;

    // Getters y Setters

    public BigDecimal getTotalIngresos() {
        return totalIngresos;
    }

    public void setTotalIngresos(BigDecimal totalIngresos) {
        this.totalIngresos = totalIngresos;
    }

    public BigDecimal getTotalEgresos() {
        return totalEgresos;
    }

    public void setTotalEgresos(BigDecimal totalEgresos) {
        this.totalEgresos = totalEgresos;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public BigDecimal getIngresosPorCitas() {
        return ingresosPorCitas;
    }

    public void setIngresosPorCitas(BigDecimal ingresosPorCitas) {
        this.ingresosPorCitas = ingresosPorCitas;
    }

    public BigDecimal getEgresosPlanilla() {
        return egresosPlanilla;
    }

    public void setEgresosPlanilla(BigDecimal egresosPlanilla) {
        this.egresosPlanilla = egresosPlanilla;
    }

    public BigDecimal getEgresosInsumos() {
        return egresosInsumos;
    }

    public void setEgresosInsumos(BigDecimal egresosInsumos) {
        this.egresosInsumos = egresosInsumos;
    }

    public long getTotalPagos() {
        return totalPagos;
    }

    public void setTotalPagos(long totalPagos) {
        this.totalPagos = totalPagos;
    }

    public long getTotalCitasPagadas() {
        return totalCitasPagadas;
    }

    public void setTotalCitasPagadas(long totalCitasPagadas) {
        this.totalCitasPagadas = totalCitasPagadas;
    }
}