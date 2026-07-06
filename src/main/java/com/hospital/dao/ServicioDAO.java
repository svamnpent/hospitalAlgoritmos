package com.hospital.dao;

import com.hospital.entidades.Servicio;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.LinkedList;

@Repository
public class ServicioDAO {

    public LinkedList<Servicio> listarServicios() {
        LinkedList<Servicio> lista = new LinkedList<>();
        String sql = "SELECT id_servicio, nombre, duracion_minutos FROM servicios";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Servicio s = new Servicio();
                s.setIdServicio(rs.getInt("id_servicio"));
                s.setNombreServicio(rs.getString("nombre"));
                s.setDuracionMinutos(rs.getInt("duracion_minutos"));
                lista.add(s);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar servicios: " + e.getMessage());
        }
        return lista;
    }
}
