package com.hospital.dao;

import com.hospital.entidades.Paciente;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.LinkedList;

@Repository
public class PacienteDAO {

    public Paciente buscarPorId(int id) {
        Paciente pac = null;
        String sql = "SELECT p.*, pac.historia_clinica, pac.tipo_seguro, pac.grupo_sanguineo " +
                "FROM personas p " +
                "JOIN pacientes pac ON p.id_persona = pac.id_paciente " +
                "WHERE p.id_persona = ?";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                pac = new Paciente();
                pac.setIdPersona(rs.getInt("id_persona"));
                pac.setNombre(rs.getString("nombre"));
                pac.setApellido(rs.getString("apellido"));
                pac.setDni(rs.getString("dni"));
                pac.setTelefono(rs.getString("telefono"));
                pac.setHistoriaClinica(rs.getString("historia_clinica"));
                pac.setTipoSeguro(rs.getString("tipo_seguro"));
                pac.setGrupoSanguineo(rs.getString("grupo_sanguineo"));
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar paciente por id: " + e.getMessage());
        }
        return pac;
    }

    public Paciente buscarPorDni(String dni) {
        Paciente pac = null;
        String sql = "SELECT p.*, pac.historia_clinica, pac.tipo_seguro " +
                "FROM personas p " +
                "JOIN pacientes pac ON p.id_persona = pac.id_paciente " +
                "WHERE p.dni = ?";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, dni);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                pac = new Paciente();
                pac.setIdPersona(rs.getInt("id_persona"));
                pac.setNombre(rs.getString("nombre"));
                pac.setApellido(rs.getString("apellido"));
                pac.setDni(rs.getString("dni"));
                pac.setHistoriaClinica(rs.getString("historia_clinica"));
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar paciente: " + e.getMessage());
        }
        return pac;
    }

    public int registrarPaciente(Paciente pac) {
        int idGenerado = -1;
        String sqlPersona = "INSERT INTO personas (dni, nombre, apellido, telefono, fecha_nacimiento, direccion, genero) VALUES (?, ?, ?, ?, ?, ?, ?::genero_enum)";
        String sqlPaciente = "INSERT INTO pacientes (id_paciente, historia_clinica, tipo_seguro, grupo_sanguineo) VALUES (?, ?, ?, ?)";

        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try (PreparedStatement psPersona = con.prepareStatement(sqlPersona, Statement.RETURN_GENERATED_KEYS)) {
                psPersona.setString(1, pac.getDni());
                psPersona.setString(2, pac.getNombre());
                psPersona.setString(3, pac.getApellido());
                psPersona.setString(4, pac.getTelefono());
                psPersona.setDate(5, pac.getFechaNacimiento());
                psPersona.setString(6, pac.getDireccion());
                psPersona.setString(7, pac.getGenero());
                psPersona.executeUpdate();

                ResultSet rs = psPersona.getGeneratedKeys();
                if (rs.next()) idGenerado = rs.getInt(1);
            }

            if (idGenerado != -1) {
                try (PreparedStatement psPaciente = con.prepareStatement(sqlPaciente)) {
                    psPaciente.setInt(1, idGenerado);
                    psPaciente.setString(2, pac.getHistoriaClinica());
                    psPaciente.setString(3, pac.getTipoSeguro());
                    psPaciente.setString(4, pac.getGrupoSanguineo());
                    psPaciente.executeUpdate();
                }
                con.commit();
            } else {
                con.rollback();
            }
        } catch (SQLException e) {
            System.err.println("Error al registrar paciente: " + e.getMessage());
            idGenerado = -1;
        }
        return idGenerado;
    }

    public boolean modificarPaciente(Paciente p) {
        String sqlPersona = "UPDATE personas SET dni=?, nombre=?, apellido=?, telefono=?, fecha_nacimiento=?, direccion=?, genero=?::genero_enum WHERE id_persona=?";
        String sqlPaciente = "UPDATE pacientes SET tipo_seguro=?, grupo_sanguineo=? WHERE id_paciente=?";

        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps1 = con.prepareStatement(sqlPersona)) {
                ps1.setString(1, p.getDni());
                ps1.setString(2, p.getNombre());
                ps1.setString(3, p.getApellido());
                ps1.setString(4, p.getTelefono());
                ps1.setDate(5, new java.sql.Date(p.getFechaNacimiento().getTime()));
                ps1.setString(6, p.getDireccion());
                ps1.setString(7, p.getGenero());
                ps1.setInt(8, p.getIdPersona());
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = con.prepareStatement(sqlPaciente)) {
                ps2.setString(1, p.getTipoSeguro());
                ps2.setString(2, p.getGrupoSanguineo());
                ps2.setInt(3, p.getIdPersona());
                ps2.executeUpdate();
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al modificar paciente: " + e.getMessage());
            return false;
        }
    }

    public LinkedList<Paciente> listarYOrdenarPacientes(String criterioOrden) {
        LinkedList<Paciente> lista = new LinkedList<>();
        String columnaOrder = switch (criterioOrden) {
            case "nombre" -> "p.nombre";
            case "fecha_nacimiento" -> "p.fecha_nacimiento";
            case "dni" -> "p.dni";
            default -> "p.apellido";
        };

        String sql = "SELECT p.id_persona, p.dni, p.nombre, p.apellido, pa.grupo_sanguineo, " +
                "p.direccion, p.telefono, p.genero, pa.tipo_seguro, p.fecha_nacimiento " +
                "FROM pacientes pa " +
                "JOIN personas p ON pa.id_paciente = p.id_persona " +
                "ORDER BY " + columnaOrder + " ASC";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Paciente p = new Paciente();
                p.setIdPersona(rs.getInt("id_persona"));
                p.setDni(rs.getString("dni"));
                p.setNombre(rs.getString("nombre"));
                p.setApellido(rs.getString("apellido"));
                p.setGrupoSanguineo(rs.getString("grupo_sanguineo"));
                p.setDireccion(rs.getString("direccion"));
                p.setTelefono(rs.getString("telefono"));
                p.setGenero(rs.getString("genero"));
                p.setTipoSeguro(rs.getString("tipo_seguro"));
                p.setFechaNacimiento(rs.getDate("fecha_nacimiento"));
                lista.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar pacientes: " + e.getMessage());
        }
        return lista;
    }

    public LinkedList<Paciente> buscarPacienteTexto(String texto) {
        LinkedList<Paciente> lista = new LinkedList<>();
        String sql = "SELECT p.id_persona, p.dni, p.nombre, p.apellido, pa.grupo_sanguineo, " +
                "p.direccion, p.telefono, p.genero, pa.tipo_seguro, p.fecha_nacimiento " +
                "FROM pacientes pa " +
                "JOIN personas p ON pa.id_paciente = p.id_persona " +
                "WHERE p.dni = ? OR p.nombre LIKE ? OR p.apellido LIKE ?";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, texto);
            ps.setString(2, "%" + texto + "%");
            ps.setString(3, "%" + texto + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Paciente p = new Paciente();
                p.setIdPersona(rs.getInt("id_persona"));
                p.setDni(rs.getString("dni"));
                p.setNombre(rs.getString("nombre"));
                p.setApellido(rs.getString("apellido"));
                p.setGrupoSanguineo(rs.getString("grupo_sanguineo"));
                p.setDireccion(rs.getString("direccion"));
                p.setTelefono(rs.getString("telefono"));
                p.setGenero(rs.getString("genero"));
                p.setTipoSeguro(rs.getString("tipo_seguro"));
                p.setFechaNacimiento(rs.getDate("fecha_nacimiento"));
                lista.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar paciente: " + e.getMessage());
        }
        return lista;
    }

    public LinkedList<Paciente> listarPacientesPorMedico(int idMedico) {
        LinkedList<Paciente> lista = new LinkedList<>();
        String sql = "SELECT DISTINCT p.id_persona, p.dni, p.nombre, p.apellido, " +
                "p.telefono, p.fecha_nacimiento, p.direccion, p.genero, " +
                "pa.historia_clinica, pa.tipo_seguro, pa.grupo_sanguineo, pa.alergias " +
                "FROM citas c " +
                "JOIN pacientes pa ON c.id_paciente = pa.id_paciente " +
                "JOIN personas p ON pa.id_paciente = p.id_persona " +
                "WHERE c.id_medico = ? " +
                "ORDER BY p.apellido, p.nombre";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idMedico);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Paciente p = new Paciente();
                p.setIdPersona(rs.getInt("id_persona"));
                p.setDni(rs.getString("dni"));
                p.setNombre(rs.getString("nombre"));
                p.setApellido(rs.getString("apellido"));
                p.setTelefono(rs.getString("telefono"));
                p.setFechaNacimiento(rs.getDate("fecha_nacimiento"));
                p.setDireccion(rs.getString("direccion"));
                p.setGenero(rs.getString("genero"));
                p.setHistoriaClinica(rs.getString("historia_clinica"));
                p.setTipoSeguro(rs.getString("tipo_seguro"));
                p.setGrupoSanguineo(rs.getString("grupo_sanguineo"));
                p.setAlergias(rs.getString("alergias"));
                lista.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar pacientes del medico: " + e.getMessage());
        }
        return lista;
    }
}
