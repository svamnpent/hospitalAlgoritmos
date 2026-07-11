package com.hospital.dao;

import com.hospital.entidades.Cita;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.LinkedList;

@Repository
public class CitaDAO {

    public boolean registrarCita(Cita cita) {
        String sql = "INSERT INTO citas (id_paciente, id_medico, id_servicio, fecha_cita, estado) VALUES (?, ?, ?, ?, 'PENDIENTE')";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, cita.getIdPaciente());
            ps.setInt(2, cita.getIdMedico());
            ps.setInt(3, cita.getIdServicio());
            ps.setTimestamp(4, cita.getFechaCitaCom());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al insertar cita: " + e.getMessage());
            return false;
        }
    }

    public LinkedList<Cita> listarCitasPendientesHoy(int idMedico) {
        LinkedList<Cita> lista = new LinkedList<>();
        String sql = "SELECT c.id_cita, c.id_paciente, c.id_servicio, c.fecha_cita, c.estado, " +
                "p.nombre, p.apellido, s.nombre AS nombre_servicio " +
                "FROM citas c " +
                "JOIN personas p ON c.id_paciente = p.id_persona " +
                "JOIN servicios s ON c.id_servicio = s.id_servicio " +
                "WHERE c.id_medico = ? AND c.estado = 'PENDIENTE' " +
                "AND DATE(c.fecha_cita) = (CURRENT_DATE + INTERVAL '5 HOURS') " +
                "ORDER BY c.fecha_cita ASC";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMedico);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Cita c = new Cita();
                c.setIdCita(rs.getInt("id_cita"));
                c.setIdPaciente(rs.getInt("id_paciente"));
                c.setIdServicio(rs.getInt("id_servicio"));
                c.setFechaCitaCom(rs.getTimestamp("fecha_cita"));
                c.setEstado(rs.getString("estado"));
                c.setNombrePaciente(rs.getString("nombre") + " " + rs.getString("apellido"));
                c.setNombreServicio(rs.getString("nombre_servicio"));
                lista.add(c);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar cola de atención: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    public boolean actualizarEstadoCita(int idCita, String estado) {
        String sql = "UPDATE citas SET estado = ?::estado_cita_enum WHERE id_cita = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, estado);
            ps.setInt(2, idCita);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar estado de cita: " + e.getMessage());
            return false;
        }
    }


    public LinkedList<Cita> listarCitasPendientesPorPacienteConMedico(int idPaciente) {
        LinkedList<Cita> lista = new LinkedList<>();
        String sql = """
            SELECT c.id_cita, c.fecha_cita, c.estado,
                   s.nombre AS nombre_servicio,
                   CONCAT(pe.nombre, ' ', pe.apellido) AS nombre_medico
            FROM citas c
            JOIN servicios s ON c.id_servicio = s.id_servicio
            JOIN personal_medico pm ON c.id_medico = pm.id_medico
            JOIN personas pe ON pm.id_medico = pe.id_persona
            WHERE c.id_paciente = ? AND c.estado = 'PENDIENTE'
            ORDER BY c.fecha_cita ASC
            """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPaciente);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Cita c = new Cita();
                c.setIdCita(rs.getInt("id_cita"));
                c.setFechaCitaCom(rs.getTimestamp("fecha_cita"));
                c.setEstado(rs.getString("estado"));
                c.setNombreServicio(rs.getString("nombre_servicio"));
                c.setNombreMedico(rs.getString("nombre_medico"));
                lista.add(c);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar citas pendientes con medico: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    public LinkedList<Cita> listarCitasPendientesPorPaciente(int idPaciente) {
        LinkedList<Cita> lista = new LinkedList<>();
        String sql = """
        SELECT c.id_cita, c.fecha_cita, c.estado,
               s.nombre AS nombre_servicio
        FROM citas c
        JOIN servicios s ON c.id_servicio = s.id_servicio
        WHERE c.id_paciente = ? AND c.estado = 'PENDIENTE'
        ORDER BY c.fecha_cita ASC
        """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPaciente);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Cita c = new Cita();
                c.setIdCita(rs.getInt("id_cita"));
                c.setFechaCitaCom(rs.getTimestamp("fecha_cita"));
                c.setEstado(rs.getString("estado"));
                c.setNombreServicio(rs.getString("nombre_servicio"));
                lista.add(c);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar citas pendientes del paciente: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    public LinkedList<Cita> listarCitasAtendidasPorPaciente(int idPaciente) {
        LinkedList<Cita> lista = new LinkedList<>();

        String sql = """
        SELECT c.id_cita, c.fecha_cita, c.estado,
               s.nombre AS nombre_servicio
        FROM citas c
        JOIN servicios s ON c.id_servicio = s.id_servicio
        WHERE c.id_paciente = ? AND c.estado = 'ATENDIDA'
        ORDER BY c.fecha_cita DESC
        """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPaciente);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Cita c = new Cita();
                c.setIdCita(rs.getInt("id_cita"));
                c.setFechaCitaCom(rs.getTimestamp("fecha_cita"));
                c.setEstado(rs.getString("estado"));
                c.setNombreServicio(rs.getString("nombre_servicio"));
                lista.add(c);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar citas atendidas del paciente: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

}