package com.hospital.dao;

import com.hospital.entidades.Empleado;
import com.hospital.entidades.ReportePersonal;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.sql.Date;
import java.util.*;

@Repository
public class RRHHDAO {

    public ReportePersonal obtenerResumenPersonal() {
        ReportePersonal resumen = new ReportePersonal();
        String sql = """
            SELECT 
                (SELECT COUNT(*) FROM empleados) AS total,
                (SELECT COUNT(*) FROM empleados WHERE estado_contrato = 'ACTIVO') AS activos,
                (SELECT COUNT(*) FROM empleados WHERE estado_contrato = 'INACTIVO') AS inactivos,
                (SELECT COUNT(*) FROM empleados WHERE id_rol = 3) AS doctores,
                (SELECT COUNT(*) FROM empleados WHERE id_rol = 2) AS recepcionistas,
                (SELECT COUNT(*) FROM empleados WHERE id_rol = 1) AS admins,
                (SELECT COUNT(*) FROM empleados WHERE id_rol = 4) AS contadores,
                (SELECT COUNT(*) FROM empleados WHERE id_rol = 5) AS rrhh,
                (SELECT COALESCE(SUM(sueldo_base), 0) FROM empleados WHERE estado_contrato = 'ACTIVO') AS planilla
            """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                resumen.setTotalEmpleados(rs.getLong("total"));
                resumen.setActivos(rs.getLong("activos"));
                resumen.setInactivos(rs.getLong("inactivos"));
                resumen.setDoctores(rs.getLong("doctores"));
                resumen.setRecepcionistas(rs.getLong("recepcionistas"));
                resumen.setAdmins(rs.getLong("admins"));
                resumen.setContadores(rs.getLong("contadores"));
                resumen.setRrhh(rs.getLong("rrhh"));
                resumen.setTotalPlanilla(rs.getDouble("planilla"));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener resumen personal: " + e.getMessage());
        }
        return resumen;
    }

    public List<Empleado> listarEmpleados(String estado, Integer idRol) {
        List<Empleado> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT p.id_persona, p.dni, p.nombre, p.apellido, p.telefono, 
                   p.fecha_nacimiento, p.direccion, p.genero,
                   e.id_rol, e.sueldo_base, e.fecha_contratacion, e.estado_contrato,
                   e.dia_descanso, e.hora_entrada, e.hora_salida,
                   r.nombre AS rol_nombre
            FROM empleados e
            JOIN personas p ON e.id_empleado = p.id_persona
            JOIN roles r ON e.id_rol = r.id_rol
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();
        if (estado != null && !estado.isEmpty()) {
            sql.append(" AND e.estado_contrato = ?::estado_contrato_enum");
            params.add(estado);
        }
        if (idRol != null && idRol > 0) {
            sql.append(" AND e.id_rol = ?");
            params.add(idRol);
        }
        sql.append(" ORDER BY p.apellido, p.nombre");

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Empleado emp = new Empleado();
                emp.setIdPersona(rs.getInt("id_persona"));
                emp.setDni(rs.getString("dni"));
                emp.setNombre(rs.getString("nombre"));
                emp.setApellido(rs.getString("apellido"));
                emp.setTelefono(rs.getString("telefono"));
                emp.setFechaNacimiento(rs.getDate("fecha_nacimiento"));
                emp.setDireccion(rs.getString("direccion"));
                emp.setGenero(rs.getString("genero"));
                emp.setIdRol(rs.getInt("id_rol"));
                emp.setSueldoBase(rs.getDouble("sueldo_base"));
                emp.setFechaContratacion(rs.getDate("fecha_contratacion"));
                emp.setEstadoContrato(rs.getString("estado_contrato"));
                emp.setDiaDescanso(rs.getString("dia_descanso"));
                emp.setHoraEntrada(rs.getTime("hora_entrada"));
                emp.setHoraSalida(rs.getTime("hora_salida"));
                lista.add(emp);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar empleados: " + e.getMessage());
        }
        return lista;
    }

    public List<Map<String, Object>> obtenerDistribucionPorRol() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        String sql = """
            SELECT r.nombre AS rol, COUNT(e.id_empleado) AS total
            FROM empleados e
            JOIN roles r ON e.id_rol = r.id_rol
            GROUP BY r.id_rol
            ORDER BY total DESC
            """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("rol", rs.getString("rol"));
                row.put("total", rs.getLong("total"));
                resultado.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener distribución por rol: " + e.getMessage());
        }
        return resultado;
    }

    // ── CONTRATAR NUEVO EMPLEADO ─────────────────────────────────
    public boolean contratarEmpleado(String dni, String nombre, String apellido, String telefono,
                                     Date fechaNacimiento, String direccion, String genero,
                                     int idRol, double sueldoBase, Date fechaContratacion,
                                     String diaDescanso, Time horaEntrada, Time horaSalida,
                                     String username, String password) {
        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);

            // 1. Insertar en personas
            String sqlPersona = "INSERT INTO personas (dni, nombre, apellido, telefono, fecha_nacimiento, direccion, genero) VALUES (?, ?, ?, ?, ?, ?, ?::genero_enum)";
            int idPersona = -1;
            try (PreparedStatement ps = con.prepareStatement(sqlPersona, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, dni);
                ps.setString(2, nombre);
                ps.setString(3, apellido);
                ps.setString(4, telefono);
                ps.setDate(5, fechaNacimiento);
                ps.setString(6, direccion);
                ps.setString(7, genero);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) idPersona = rs.getInt(1);
            }

            if (idPersona == -1) {
                con.rollback();
                return false;
            }

            // 2. Insertar en empleados
            String sqlEmpleado = "INSERT INTO empleados (id_empleado, id_rol, sueldo_base, fecha_contratacion, estado_contrato, dia_descanso, hora_entrada, hora_salida) VALUES (?, ?, ?, ?, 'ACTIVO', ?::dia_semana_enum, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sqlEmpleado)) {
                ps.setInt(1, idPersona);
                ps.setInt(2, idRol);
                ps.setDouble(3, sueldoBase);
                ps.setDate(4, fechaContratacion);
                ps.setString(5, diaDescanso);
                ps.setTime(6, horaEntrada);
                ps.setTime(7, horaSalida);
                ps.executeUpdate();
            }

            // 3. Insertar en usuarios
            String sqlUsuario = "INSERT INTO usuarios (id_empleado, username, password, activo) VALUES (?, ?, ?, true)";
            try (PreparedStatement ps = con.prepareStatement(sqlUsuario)) {
                ps.setInt(1, idPersona);
                ps.setString(2, username);
                ps.setString(3, password);
                ps.executeUpdate();
            }

            // 4. Si es médico, insertar en personal_medico
            if (idRol == 3) {
                String sqlMedico = "INSERT INTO personal_medico (id_medico, id_especialidad, colegiatura) VALUES (?, 1, ?)";
                try (PreparedStatement ps = con.prepareStatement(sqlMedico)) {
                    ps.setInt(1, idPersona);
                    ps.setString(2, "CMP-" + System.currentTimeMillis());
                    ps.executeUpdate();
                }
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al contratar: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizarEstadoContrato(int idEmpleado, String estado) {
        String sql = "UPDATE empleados SET estado_contrato = ?::estado_contrato_enum WHERE id_empleado = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, estado);
            ps.setInt(2, idEmpleado);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar estado: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizarHorario(int idEmpleado, String diaDescanso, Time horaEntrada, Time horaSalida) {
        String sql = "UPDATE empleados SET dia_descanso = ?::dia_semana_enum, hora_entrada = ?, hora_salida = ? WHERE id_empleado = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, diaDescanso);
            ps.setTime(2, horaEntrada);
            ps.setTime(3, horaSalida);
            ps.setInt(4, idEmpleado);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar horario: " + e.getMessage());
            return false;
        }
    }
}