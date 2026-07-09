// CajaDAO.java
package com.hospital.dao;

import com.hospital.entidades.CajaMovimiento;
import com.hospital.entidades.ReporteFinanciero;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

@Repository
public class CajaDAO {

    public ReporteFinanciero obtenerResumenFinanciero() {
        ReporteFinanciero resumen = new ReporteFinanciero();
        String sql = """
            SELECT 
                (SELECT COALESCE(SUM(monto), 0) FROM caja_movimientos WHERE tipo = 'INGRESO') AS ingresos,
                (SELECT COALESCE(SUM(monto), 0) FROM caja_movimientos WHERE tipo = 'EGRESO') AS egresos,
                (SELECT COALESCE(SUM(monto), 0) FROM caja_movimientos WHERE tipo = 'INGRESO' AND categoria = 'PAGO_CITA') AS ingresos_citas,
                (SELECT COALESCE(SUM(monto), 0) FROM caja_movimientos WHERE tipo = 'EGRESO' AND categoria = 'PLANILLA') AS egresos_planilla,
                (SELECT COALESCE(SUM(monto), 0) FROM caja_movimientos WHERE tipo = 'EGRESO' AND categoria = 'INSUMOS') AS egresos_insumos,
                (SELECT COUNT(*) FROM pagos_citas) AS total_pagos,
                (SELECT COUNT(DISTINCT id_cita) FROM pagos_citas) AS citas_pagadas
            """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                BigDecimal ingresos = rs.getBigDecimal("ingresos");
                BigDecimal egresos = rs.getBigDecimal("egresos");
                resumen.setTotalIngresos(ingresos);
                resumen.setTotalEgresos(egresos);
                resumen.setSaldo(ingresos.subtract(egresos));
                resumen.setIngresosPorCitas(rs.getBigDecimal("ingresos_citas"));
                resumen.setEgresosPlanilla(rs.getBigDecimal("egresos_planilla"));
                resumen.setEgresosInsumos(rs.getBigDecimal("egresos_insumos"));
                resumen.setTotalPagos(rs.getLong("total_pagos"));
                resumen.setTotalCitasPagadas(rs.getLong("citas_pagadas"));
            }
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return resumen;
    }

    public List<CajaMovimiento> listarMovimientos(boolean soloIngresos, boolean soloEgresos,
                                                  String categoria, String fechaInicio, String fechaFin) {
        List<CajaMovimiento> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT m.*, CONCAT(p.nombre, ' ', p.apellido) AS nombre_contador
            FROM caja_movimientos m
            JOIN empleados e ON m.id_contador = e.id_empleado
            JOIN personas p ON e.id_empleado = p.id_persona
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (soloIngresos) { sql.append(" AND m.tipo = 'INGRESO'"); }
        if (soloEgresos) { sql.append(" AND m.tipo = 'EGRESO'"); }
        if (categoria != null && !categoria.isEmpty()) {
            sql.append(" AND m.categoria = ?::categoria_movimiento_enum");
            params.add(categoria);
        }
        if (fechaInicio != null && !fechaInicio.isEmpty()) {
            sql.append(" AND DATE(m.fecha) >= ?");
            params.add(fechaInicio);
        }
        if (fechaFin != null && !fechaFin.isEmpty()) {
            sql.append(" AND DATE(m.fecha) <= ?");
            params.add(fechaFin);
        }
        sql.append(" ORDER BY m.fecha DESC");

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                CajaMovimiento m = new CajaMovimiento();
                m.setIdMovimiento(rs.getInt("id_movimiento"));
                m.setMonto(rs.getBigDecimal("monto"));
                m.setTipo(rs.getString("tipo"));
                m.setCategoria(rs.getString("categoria"));
                m.setDescripcion(rs.getString("descripcion"));
                m.setFecha(rs.getTimestamp("fecha"));
                m.setIdContador(rs.getInt("id_contador"));
                m.setNombreContador(rs.getString("nombre_contador"));
                lista.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return lista;
    }

    public boolean registrarMovimiento(CajaMovimiento movimiento) {
        String sql = """       
            INSERT INTO caja_movimientos (monto, tipo, categoria, descripcion, id_contador)
            VALUES (?, ?::tipo_movimiento_enum, ?::categoria_movimiento_enum, ?, ?)
            """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBigDecimal(1, movimiento.getMonto());
            ps.setString(2, movimiento.getTipo());
            ps.setString(3, movimiento.getCategoria());
            ps.setString(4, movimiento.getDescripcion());
            ps.setInt(5, movimiento.getIdContador());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
            return false;
        }
    }

    public List<Map<String, Object>> obtenerIngresosPorMes(int year) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        String sql = """
           
                SELECT EXTRACT(MONTH FROM fecha) AS mes, ...
                                 FROM caja_movimientos
                                 WHERE tipo = 'INGRESO' AND EXTRACT(YEAR FROM fecha) = ?
                                 GROUP BY EXTRACT(MONTH FROM fecha)
            ORDER BY mes
            """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, year);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("mes", rs.getInt("mes"));
                row.put("total", rs.getBigDecimal("total"));
                resultado.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return resultado;
    }

    public List<Map<String, Object>> obtenerEgresosPorCategoria() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        String sql = """
            SELECT categoria, COALESCE(SUM(monto), 0) AS total
            FROM caja_movimientos
            WHERE tipo = 'EGRESO'
            GROUP BY categoria
            ORDER BY total DESC
            """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("categoria", rs.getString("categoria"));
                row.put("total", rs.getBigDecimal("total"));
                resultado.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return resultado;
    }
}