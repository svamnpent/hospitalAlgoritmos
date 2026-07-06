// ReporteDAO.java
package com.hospital.dao;

import com.hospital.entidades.ReporteCitas;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.util.*;

@Repository
public class ReporteDAO {

    public ReporteCitas obtenerEstadisticasGenerales() {
        ReporteCitas reporte = new ReporteCitas();
        String sql = """
            SELECT 
                (SELECT COUNT(*) FROM citas) AS total_citas,
                (SELECT COUNT(*) FROM citas WHERE estado = 'ATENDIDA') AS atendidas,
                (SELECT COUNT(*) FROM citas WHERE estado = 'PENDIENTE') AS pendientes,
                (SELECT COUNT(*) FROM citas WHERE estado = 'CANCELADA') AS canceladas,
                (SELECT COUNT(*) FROM citas WHERE DATE(fecha_cita) = CURDATE()) AS citas_hoy,
                (SELECT COUNT(*) FROM citas WHERE YEARWEEK(fecha_cita) = YEARWEEK(CURDATE())) AS citas_semana,
                (SELECT COUNT(*) FROM citas WHERE MONTH(fecha_cita) = MONTH(CURDATE()) AND YEAR(fecha_cita) = YEAR(CURDATE())) AS citas_mes,
                (SELECT COUNT(*) FROM pacientes) AS pacientes_total,
                (SELECT COUNT(*) FROM personal_medico pm JOIN empleados e ON pm.id_medico = e.id_empleado WHERE e.estado_contrato = 'ACTIVO') AS medicos_activos,
                (SELECT COUNT(*) FROM empleados WHERE estado_contrato = 'ACTIVO') AS empleados_activos
            """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                reporte.setTotalCitas(rs.getLong("total_citas"));
                reporte.setCitasAtendidas(rs.getLong("atendidas"));
                reporte.setCitasPendientes(rs.getLong("pendientes"));
                reporte.setCitasCanceladas(rs.getLong("canceladas"));
                reporte.setCitasHoy(rs.getLong("citas_hoy"));
                reporte.setCitasEstaSemana(rs.getLong("citas_semana"));
                reporte.setCitasEsteMes(rs.getLong("citas_mes"));
                reporte.setPacientesRegistrados(rs.getLong("pacientes_total"));
                reporte.setMedicosActivos(rs.getLong("medicos_activos"));
                reporte.setEmpleadosActivos(rs.getLong("empleados_activos"));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener estadísticas: " + e.getMessage());
        }
        return reporte;
    }

    public List<Map<String, Object>> obtenerCitasPorEspecialidad() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        String sql = """
            SELECT e.nombre AS especialidad, COUNT(c.id_cita) AS total
            FROM citas c
            JOIN servicios s ON c.id_servicio = s.id_servicio
            JOIN especialidades e ON s.id_especialidad = e.id_especialidad
            GROUP BY e.id_especialidad
            ORDER BY total DESC
            """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("especialidad", rs.getString("especialidad"));
                row.put("total", rs.getLong("total"));
                resultado.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return resultado;
    }

    public List<Map<String, Object>> obtenerCitasPorMedico() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        String sql = """
            SELECT CONCAT(p.nombre, ' ', p.apellido) AS medico, 
                   COUNT(c.id_cita) AS total,
                   SUM(CASE WHEN c.estado = 'ATENDIDA' THEN 1 ELSE 0 END) AS atendidas
            FROM citas c
            JOIN personal_medico pm ON c.id_medico = pm.id_medico
            JOIN personas p ON pm.id_medico = p.id_persona
            GROUP BY pm.id_medico
            ORDER BY total DESC
            """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("medico", rs.getString("medico"));
                row.put("total", rs.getLong("total"));
                row.put("atendidas", rs.getLong("atendidas"));
                resultado.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return resultado;
    }

    public List<Map<String, Object>> obtenerCitasPorEstado() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        String sql = """
            SELECT estado, COUNT(*) AS total
            FROM citas
            GROUP BY estado
            """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("estado", rs.getString("estado"));
                row.put("total", rs.getLong("total"));
                resultado.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return resultado;
    }

    public List<Map<String, Object>> obtenerCitasPorMes(int year) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        String sql = """
            SELECT MONTH(fecha_cita) AS mes, 
                   COUNT(*) AS total,
                   SUM(CASE WHEN estado = 'ATENDIDA' THEN 1 ELSE 0 END) AS atendidas
            FROM citas
            WHERE YEAR(fecha_cita) = ?
            GROUP BY MONTH(fecha_cita)
            ORDER BY mes
            """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, year);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("mes", rs.getInt("mes"));
                row.put("total", rs.getLong("total"));
                row.put("atendidas", rs.getLong("atendidas"));
                resultado.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return resultado;
    }
}