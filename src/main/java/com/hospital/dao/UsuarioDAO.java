package com.hospital.dao;

import com.hospital.entidades.Empleado;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.LinkedList;

@Repository
public class UsuarioDAO {

    public Empleado obtenerPersonalPorUsername(String username) {
        Empleado emp = null;
        String sql = "SELECT p.id_persona, e.id_rol, p.nombre, p.apellido, p.dni, p.telefono, " +
                "e.dia_descanso, u.username, u.password " +
                "FROM usuarios u " +
                "JOIN empleados e ON u.id_empleado = e.id_empleado " +
                "JOIN personas p ON e.id_empleado = p.id_persona " +
                "WHERE u.username = ? AND u.activo = true";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                emp = new Empleado();
                emp.setIdPersona(rs.getInt("id_persona"));
                emp.setIdRol(rs.getInt("id_rol"));
                emp.setNombre(rs.getString("nombre"));
                emp.setApellido(rs.getString("apellido"));
                emp.setDni(rs.getString("dni"));
                emp.setTelefono(rs.getString("telefono"));
                emp.setDiaDescanso(rs.getString("dia_descanso"));
                emp.setUsername(rs.getString("username"));
                emp.setPassword(rs.getString("password"));
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar usuario: " + e.getMessage());
        }
        return emp;
    }

    public Empleado obtenerPersonalPorCredenciales(String user, String pass) {
        Empleado emp = null;
        String sql = "SELECT p.id_persona, e.id_rol, p.nombre, p.apellido, p.dni, p.telefono, e.dia_descanso " +
                "FROM usuarios u " +
                "JOIN empleados e ON u.id_empleado = e.id_empleado " +
                "JOIN personas p ON e.id_empleado = p.id_persona " +
                "WHERE u.username = ? AND u.password = ? AND u.activo = true";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, user);
            ps.setString(2, pass);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                emp = new Empleado();
                emp.setIdPersona(rs.getInt("id_persona"));
                emp.setIdRol(rs.getInt("id_rol"));
                emp.setNombre(rs.getString("nombre"));
                emp.setApellido(rs.getString("apellido"));
                emp.setDni(rs.getString("dni"));
                emp.setTelefono(rs.getString("telefono"));
                emp.setDiaDescanso(rs.getString("dia_descanso"));
            }
        } catch (SQLException e) {
            System.err.println("Error en login: " + e.getMessage());
        }
        return emp;
    }
    public LinkedList<Object[]> listarPersonalParaArbol() {
        LinkedList<Object[]> lista = new LinkedList<>();
        String sql = "SELECT p.dni, p.nombre, p.apellido, p.telefono, r.nombre as rol, u.username " +
                "FROM usuarios u " +
                "JOIN empleados e ON u.id_empleado = e.id_empleado " +
                "JOIN personas p ON e.id_empleado = p.id_persona " +
                "JOIN roles r ON e.id_rol = r.id_rol " +
                "WHERE u.activo = true ORDER BY p.dni";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Object[] fila = new Object[6];
                fila[0] = rs.getString("dni");
                fila[1] = rs.getString("nombre");
                fila[2] = rs.getString("apellido");
                fila[3] = rs.getString("telefono");
                fila[4] = rs.getString("rol");
                fila[5] = rs.getString("username");
                lista.add(fila);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar personal: " + e.getMessage());
        }
        return lista;
    }
}
