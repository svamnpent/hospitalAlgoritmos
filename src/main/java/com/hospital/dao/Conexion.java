package com.hospital.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wrapper que expone la conexión del DataSource de Spring de forma estática,
 * para no modificar la lógica de los DAOs originales.
 */
@Component
public class Conexion {

    private static DataSource dataSource;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        Conexion.dataSource = dataSource;
    }

    public static Connection getConexion() throws SQLException {
        return dataSource.getConnection();
    }
}
