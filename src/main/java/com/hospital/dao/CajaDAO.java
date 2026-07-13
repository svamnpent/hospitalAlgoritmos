package com.hospital.dao;

import com.hospital.entidades.CajaMovimiento;
import com.hospital.entidades.ReporteFinanciero;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

@Repository
public class CajaDAO {

    public ReporteFinanciero obtenerResumenFinanciero() {
        ReporteFinanciero resumen = new ReporteFinanciero();
        String sql = """
            SELECT 
                (SELECT COALESCE(SUM(monto), 0) FROM caja_movimientos WHERE tipo = 'INGRESO' AND revertido = 0) AS ingresos,
                (SELECT COALESCE(SUM(monto), 0) FROM caja_movimientos WHERE tipo = 'EGRESO' AND revertido = 0) AS egresos,
                (SELECT COALESCE(SUM(monto), 0) FROM caja_movimientos WHERE tipo = 'INGRESO' AND categoria = 'PAGO_CITA' AND revertido = 0) AS ingresos_citas,
                (SELECT COALESCE(SUM(monto), 0) FROM caja_movimientos WHERE tipo = 'EGRESO' AND categoria = 'PLANILLA' AND revertido = 0) AS egresos_planilla,
                (SELECT COALESCE(SUM(monto), 0) FROM caja_movimientos WHERE tipo = 'EGRESO' AND categoria = 'INSUMOS' AND revertido = 0) AS egresos_insumos,
                (SELECT COUNT(*) FROM pagos_citas) AS total_pagos,
                (SELECT COUNT(DISTINCT id_cita) FROM pagos_citas) AS citas_pagadas
            """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                BigDecimal ingresos = rs.getBigDecimal("ingresos");
                BigDecimal egresos = rs.getBigDecimal("egresos");
                resumen.setTotalIngresos(ingresos);
                resumen.setTotalEgresos(egresos);
                resumen.setSaldo(ingresos.subtract(egresos));
                resumen.setIngresosPorCitas(rs.getBigDecimal("ingresos_citas"));
                resumen.setEgresosPlanilla(rs.getBigDecimal("egresos_planilla"));
                resumen.setEgresosInsumos(rs.getBigDecimal("egresos_insumos"));
                resumen.setTotalPagos(rs.getLong("total_pagos"));
                resumen.setTotalCitasPagadas(rs.getLong("citas_pagadas"));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener resumen financiero: " + e.getMessage());
        }
        return resumen;
    }

    public List<CajaMovimiento> listarMovimientos(boolean soloIngresos, boolean soloEgresos,
                                                  String categoria, String fechaInicio, String fechaFin) {
        List<CajaMovimiento> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT m.*, CONCAT(p.nombre, ' ', p.apellido) AS nombre_contador
            FROM caja_movimientos m
            JOIN empleados e ON m.id_contador = e.id_empleado
            JOIN personas p ON e.id_empleado = p.id_persona
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (soloIngresos) { sql.append(" AND m.tipo = 'INGRESO'"); }
        if (soloEgresos) { sql.append(" AND m.tipo = 'EGRESO'"); }
        if (categoria != null && !categoria.isEmpty()) {
            sql.append(" AND m.categoria = ?");
            params.add(categoria);
        }
        if (fechaInicio != null && !fechaInicio.isEmpty()) {
            sql.append(" AND DATE(m.fecha) >= ?");
            params.add(fechaInicio);
        }
        if (fechaFin != null && !fechaFin.isEmpty()) {
            sql.append(" AND DATE(m.fecha) <= ?");
            params.add(fechaFin);
        }
        sql.append(" ORDER BY m.id_movimiento DESC");

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                CajaMovimiento m = new CajaMovimiento();
                m.setIdMovimiento(rs.getInt("id_movimiento"));
                m.setMonto(rs.getBigDecimal("monto"));
                m.setTipo(rs.getString("tipo"));
                m.setCategoria(rs.getString("categoria"));
                m.setDescripcion(rs.getString("descripcion"));
                m.setFecha(rs.getTimestamp("fecha"));
                m.setIdContador(rs.getInt("id_contador"));
                m.setNombreContador(rs.getString("nombre_contador"));
                m.setRevertido(rs.getInt("revertido") == 1);
                m.setRevertidoPor(rs.getInt("revertido_por"));
                lista.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar movimientos: " + e.getMessage());
        }
        return lista;
    }

    public boolean registrarPagoCita(int idCita, BigDecimal monto, String metodoPago, int idContador) {
        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);

            String sqlVerificar = "SELECT estado FROM citas WHERE id_cita = ?";
            String estado = null;
            try (PreparedStatement ps = con.prepareStatement(sqlVerificar)) {
                ps.setInt(1, idCita);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) estado = rs.getString("estado");
            }

            if (estado == null || !"ATENDIDA".equals(estado)) {
                con.rollback();
                return false;
            }

            String sqlMov = "INSERT INTO caja_movimientos (monto, tipo, categoria, descripcion, id_contador, revertido, revertido_por) " +
                    "VALUES (?, CAST(? AS tipo_movimiento_enum), CAST(? AS categoria_movimiento_enum), ?, ?, 0, 0)";
            int idMov = -1;
            try (PreparedStatement ps = con.prepareStatement(sqlMov, Statement.RETURN_GENERATED_KEYS)) {
                ps.setBigDecimal(1, monto);
                ps.setString(2, "INGRESO");
                ps.setString(3, "PAGO_CITA");
                ps.setString(4, "Pago de cita #" + idCita + " - " + metodoPago);
                ps.setInt(5, idContador);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) idMov = rs.getInt(1);
            }

            if (idMov == -1) {
                con.rollback();
                return false;
            }

            String sqlPago = "INSERT INTO pagos_citas (id_cita, id_movimiento, metodo_pago) VALUES (?, ?, CAST(? AS metodo_pago_enum))";
            try (PreparedStatement ps = con.prepareStatement(sqlPago)) {
                ps.setInt(1, idCita);
                ps.setInt(2, idMov);
                ps.setString(3, metodoPago);
                ps.executeUpdate();
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al registrar pago de cita: " + e.getMessage());
            return false;
        }
    }

    // REGISTRAR PAGO A PERSONAL
    public boolean registrarPagoPersonal(int idEmpleado, BigDecimal monto, String nombreEmpleado, int idContador) {

        String sql = "INSERT INTO caja_movimientos (monto, tipo, categoria, descripcion, id_contador, revertido, revertido_por) " +
                "VALUES (?, CAST(? AS tipo_movimiento_enum), CAST(? AS categoria_movimiento_enum), ?, ?, 0, 0)";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBigDecimal(1, monto);
            ps.setString(2, "EGRESO");
            ps.setString(3, "PLANILLA");
            ps.setString(4, "Pago a " + nombreEmpleado + " (ID: " + idEmpleado + ")");
            ps.setInt(5, idContador);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al registrar pago personal: " + e.getMessage());
            return false;
        }
    }

    public String revertirMovimiento(int idMovimiento, int idContador) {
        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);

            String sqlVerificar = "SELECT tipo, categoria, monto, revertido FROM caja_movimientos WHERE id_movimiento = ?";
            String tipo = null;
            String categoria = null;
            BigDecimal monto = null;
            int revertido = 0;
            try (PreparedStatement ps = con.prepareStatement(sqlVerificar)) {
                ps.setInt(1, idMovimiento);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    tipo = rs.getString("tipo");
                    categoria = rs.getString("categoria");
                    monto = rs.getBigDecimal("monto");
                    revertido = rs.getInt("revertido");
                }
            }

            if (tipo == null) return "ERROR: Movimiento no encontrado";
            if (revertido == 1) return "ERROR: Este movimiento ya fue revertido";

            String sqlUltimo = "SELECT id_movimiento FROM caja_movimientos WHERE revertido = 0 ORDER BY id_movimiento DESC LIMIT 1";
            int ultimoId = 0;
            try (PreparedStatement ps = con.prepareStatement(sqlUltimo);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) ultimoId = rs.getInt("id_movimiento");
            }

            if (idMovimiento != ultimoId) {
                return "ERROR: Solo se puede revertir el ultimo movimiento";
            }

            String sqlRevertir = "UPDATE caja_movimientos SET revertido = 1, revertido_por = ? WHERE id_movimiento = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlRevertir)) {
                ps.setInt(1, idContador);
                ps.setInt(2, idMovimiento);
                ps.executeUpdate();
            }

            if ("PAGO_CITA".equals(categoria)) {
                String sqlEliminarPago = "DELETE FROM pagos_citas WHERE id_movimiento = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlEliminarPago)) {
                    ps.setInt(1, idMovimiento);
                    ps.executeUpdate();
                }
            }

            String tipoReversion = "INGRESO".equals(tipo) ? "EGRESO" : "INGRESO";

            String sqlAuditoria = "INSERT INTO caja_movimientos (monto, tipo, categoria, descripcion, id_contador, revertido, revertido_por) " +
                    "VALUES (?, CAST(? AS tipo_movimiento_enum), CAST(? AS categoria_movimiento_enum), ?, ?, 0, 0)";
            try (PreparedStatement ps = con.prepareStatement(sqlAuditoria)) {
                ps.setBigDecimal(1, monto);
                ps.setString(2, tipoReversion);
                ps.setString(3, "OTROS");
                ps.setString(4, "Reversion de movimiento #" + idMovimiento + " (" + tipo + " " + categoria + ")");
                ps.setInt(5, idContador);
                ps.executeUpdate();
            }

            con.commit();
            return "OK: Movimiento revertido exitosamente";
        } catch (SQLException e) {
            System.err.println("Error al revertir movimiento: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }

    //  LISTAR EMPLEADOS ACTIVOS
    public List<Object[]> listarEmpleadosActivos() {
        List<Object[]> lista = new ArrayList<>();
        String sql = """
            SELECT e.id_empleado, p.nombre, p.apellido, r.nombre AS rol, e.sueldo_base
            FROM empleados e
            JOIN personas p ON e.id_empleado = p.id_persona
            JOIN roles r ON e.id_rol = r.id_rol
            WHERE e.estado_contrato = 'ACTIVO'
            ORDER BY p.apellido, p.nombre
            """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Object[] fila = new Object[5];
                fila[0] = rs.getInt("id_empleado");
                fila[1] = rs.getString("nombre");
                fila[2] = rs.getString("apellido");
                fila[3] = rs.getString("rol");
                fila[4] = rs.getDouble("sueldo_base");
                lista.add(fila);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar empleados activos: " + e.getMessage());
        }
        return lista;
    }

    //  LISTAR EMPLEADOS POR ROL
    public List<Object[]> listarEmpleadosPorRol(int idRol) {
        List<Object[]> lista = new ArrayList<>();
        String sql = """
            SELECT e.id_empleado, p.nombre, p.apellido, e.sueldo_base
            FROM empleados e
            JOIN personas p ON e.id_empleado = p.id_persona
            WHERE e.id_rol = ? AND e.estado_contrato = 'ACTIVO'
            ORDER BY p.apellido, p.nombre
            """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idRol);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Object[] fila = new Object[4];
                fila[0] = rs.getInt("id_empleado");
                fila[1] = rs.getString("nombre");
                fila[2] = rs.getString("apellido");
                fila[3] = rs.getDouble("sueldo_base");
                lista.add(fila);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar empleados por rol: " + e.getMessage());
        }
        return lista;
    }

    // LISTAR CITAS ATENDIDAS SIN PAGAR
    public List<Object[]> listarCitasAtendidasSinPagar() {
        List<Object[]> lista = new ArrayList<>();
        String sql = """
            SELECT c.id_cita, CONCAT(p.nombre, ' ', p.apellido) AS paciente, s.nombre AS servicio, s.costo
            FROM citas c
            JOIN pacientes pac ON c.id_paciente = pac.id_paciente
            JOIN personas p ON pac.id_paciente = p.id_persona
            JOIN servicios s ON c.id_servicio = s.id_servicio
            WHERE c.estado = 'ATENDIDA'
            AND NOT EXISTS (SELECT 1 FROM pagos_citas pc WHERE pc.id_cita = c.id_cita)
            ORDER BY c.fecha_cita DESC
            """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Object[] fila = new Object[4];
                fila[0] = rs.getInt("id_cita");
                fila[1] = rs.getString("paciente");
                fila[2] = rs.getString("servicio");
                fila[3] = rs.getDouble("costo");
                lista.add(fila);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar citas sin pagar: " + e.getMessage());
        }
        return lista;
    }

    //  OBTENER MONTO DE CITA
    public BigDecimal obtenerMontoCita(int idCita) {
        String sql = "SELECT s.costo FROM citas c JOIN servicios s ON c.id_servicio = s.id_servicio WHERE c.id_cita = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idCita);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("costo");
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener monto de cita: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    public List<Map<String, Object>> obtenerIngresosPorMes(int year) {
        List<Map<String, Object>> resultado = new ArrayList<>();

        String sql = """
        SELECT EXTRACT(MONTH FROM fecha) AS mes, COALESCE(SUM(monto), 0) AS total
        FROM caja_movimientos
        WHERE tipo = 'INGRESO' AND EXTRACT(YEAR FROM fecha) = ? AND revertido = 0
        GROUP BY EXTRACT(MONTH FROM fecha)
        ORDER BY mes
        """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, year);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("mes", rs.getInt("mes"));
                row.put("total", rs.getBigDecimal("total"));
                resultado.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener ingresos por mes: " + e.getMessage());
        }
        return resultado;
    }

    public List<Map<String, Object>> obtenerEgresosPorCategoria() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        String sql = """
        SELECT categoria, COALESCE(SUM(monto), 0) AS total
        FROM caja_movimientos
        WHERE tipo = 'EGRESO' AND revertido = 0
        GROUP BY categoria
        ORDER BY total DESC
        """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("categoria", rs.getString("categoria"));
                row.put("total", rs.getBigDecimal("total"));
                resultado.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener egresos por categoria: " + e.getMessage());
        }
        return resultado;
    }
}