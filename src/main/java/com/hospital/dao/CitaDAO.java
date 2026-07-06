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
                     "WHERE c.id_medico = ? AND c.estado = 'PENDIENTE' AND DATE(c.fecha_cita) = CURDATE() " +
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
        }
        return lista;
    }

    public boolean actualizarEstadoCita(int idCita, String estado) {
        String sql = "UPDATE citas SET estado = ? WHERE id_cita = ?";
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
}
