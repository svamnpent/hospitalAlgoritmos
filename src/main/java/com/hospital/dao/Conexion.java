package com.hospital.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;


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
