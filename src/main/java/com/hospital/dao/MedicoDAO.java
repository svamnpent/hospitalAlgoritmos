package com.hospital.dao;

import com.hospital.entidades.Cita;
import com.hospital.entidades.Medico;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.LinkedList;

@Repository
public class MedicoDAO {

    public LinkedList<Medico> listarMedicos() {
        LinkedList<Medico> lista = new LinkedList<>();
        String sql = "SELECT pm.id_medico, p.nombre, p.apellido " +
                     "FROM personal_medico pm " +
                     "JOIN personas p ON pm.id_medico = p.id_persona";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Medico m = new Medico();
                m.setIdPersona(rs.getInt("id_medico"));
                m.setNombre(rs.getString("nombre"));
                m.setApellido(rs.getString("apellido"));
                lista.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar médicos: " + e.getMessage());
        }
        return lista;
    }

    public String verificarDisponibilidadMedico(int idMedico, int idServicio,
                                                 java.sql.Date fechaNueva, java.sql.Time horaInicioNueva) {
        try (Connection con = Conexion.getConexion()) {

            // 1. Verificar dia de descanso y horario laboral
            java.time.LocalDate localDate = fechaNueva.toLocalDate();
            String diaSemanaTexto = switch (localDate.getDayOfWeek()) {
                case MONDAY    -> "lunes";
                case TUESDAY   -> "martes";
                case WEDNESDAY -> "miercoles";
                case THURSDAY  -> "jueves";
                case FRIDAY    -> "viernes";
                case SATURDAY  -> "sabado";
                case SUNDAY    -> "domingo";
            };

            String sqlHorario = "SELECT dia_descanso, hora_entrada, hora_salida FROM empleados WHERE id_empleado = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlHorario)) {
                ps.setInt(1, idMedico);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String diaDescansoBD = rs.getString("dia_descanso");
                    java.sql.Time horaEntrada = rs.getTime("hora_entrada");
                    java.sql.Time horaSalida  = rs.getTime("hora_salida");

                    if (diaDescansoBD != null && diaDescansoBD.trim().equalsIgnoreCase(diaSemanaTexto)) {
                        return "El médico no trabaja los días " + diaSemanaTexto + " (es su día de descanso).";
                    }
                    if (horaInicioNueva.before(horaEntrada) || horaInicioNueva.after(horaSalida)) {
                        return "El médico no labora en ese horario. Su turno es de " + horaEntrada + " a " + horaSalida;
                    }
                }
            }

            // 2. Obtener duración del servicio
            int duracionMinutos = 30;
            String sqlServicio = "SELECT duracion_minutos FROM servicios WHERE id_servicio = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlServicio)) {
                ps.setInt(1, idServicio);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) duracionMinutos = rs.getInt("duracion_minutos");
            }

            java.sql.Time horaFinNueva = java.sql.Time.valueOf(
                horaInicioNueva.toLocalTime().plusMinutes(duracionMinutos)
            );

            // 3. Verificar solapamiento con citas existentes
            String sqlCitas = "SELECT c.fecha_cita, s.duracion_minutos FROM citas c " +
                              "JOIN servicios s ON c.id_servicio = s.id_servicio " +
                              "WHERE c.id_medico = ? AND DATE(c.fecha_cita) = ? AND c.estado != 'CANCELADA'";

            try (PreparedStatement ps = con.prepareStatement(sqlCitas)) {
                ps.setInt(1, idMedico);
                ps.setDate(2, fechaNueva);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    java.sql.Time extInicio = new java.sql.Time(rs.getTimestamp("fecha_cita").getTime());
                    java.sql.Time extFin = java.sql.Time.valueOf(
                        extInicio.toLocalTime().plusMinutes(rs.getInt("duracion_minutos"))
                    );
                    if (horaInicioNueva.before(extFin) && horaFinNueva.after(extInicio)) {
                        return "El médico ya tiene una cita ocupada en ese rango (" + extInicio + " a " + extFin + ").";
                    }
                }
            }

            return "DISPONIBLE";

        } catch (SQLException e) {
            System.err.println("Error al validar disponibilidad: " + e.getMessage());
            return "Error interno al verificar disponibilidad.";
        }
    }
}
