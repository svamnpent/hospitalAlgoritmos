package com.hospital.controlador;

import com.hospital.config.EmpleadoDetails;
import com.hospital.dao.*;
import com.hospital.entidades.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;

@Controller
@RequestMapping("/recepcionista")
public class RecepcionistaController {

    @Autowired private PacienteDAO pacienteDAO;
    @Autowired private CitaDAO citaDAO;
    @Autowired private MedicoDAO medicoDAO;
    @Autowired private ServicioDAO servicioDAO;

    //  Panel principal: lista de pacientes
    @GetMapping
    public String panel(@RequestParam(defaultValue = "apellido") String orden,
                        @AuthenticationPrincipal EmpleadoDetails detalles,
                        Model model) {

        // Carga pacientes y los inserta en el árbol BST
        LinkedList<Paciente> todosPacientes = pacienteDAO.listarYOrdenarPacientes("dni");
        ArbolPacientes arbol = new ArbolPacientes();
        for (Paciente p : todosPacientes) {
            arbol.insertar(p);
        }

        //  árbol devuelve la lista ordenada por DNI
        LinkedList<Paciente> pacientes = arbol.inorden();

        model.addAttribute("pacientes", pacientes);
        model.addAttribute("orden", orden);
        model.addAttribute("empleado", detalles.getEmpleado());
        return "recepcionista/panel";
    }


    //  Cobrar cita después de atender
    @GetMapping("/cobrar/{idCita}")
    public String formularioCobrar(@PathVariable int idCita,
                                   @AuthenticationPrincipal EmpleadoDetails detalles,
                                   Model model) {

        // Obtener datos de la cita
        String sql = """
        SELECT c.id_cita, CONCAT(p.nombre, ' ', p.apellido) AS paciente, 
               s.nombre AS servicio, s.costo, c.estado
        FROM citas c
        JOIN pacientes pac ON c.id_paciente = pac.id_paciente
        JOIN personas p ON pac.id_paciente = p.id_persona
        JOIN servicios s ON c.id_servicio = s.id_servicio
        WHERE c.id_cita = ?
        """;

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCita);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                model.addAttribute("idCita", rs.getInt("id_cita"));
                model.addAttribute("paciente", rs.getString("paciente"));
                model.addAttribute("servicio", rs.getString("servicio"));
                model.addAttribute("monto", rs.getDouble("costo"));
                model.addAttribute("estado", rs.getString("estado"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        model.addAttribute("empleado", detalles.getEmpleado());
        model.addAttribute("metodosPago", List.of("EFECTIVO", "TARJETA", "TRANSFERENCIA"));
        return "recepcionista/cobrar-cita";
    }

    @PostMapping("/cobrar")
    public String procesarCobro(@RequestParam int idCita,
                                @RequestParam String metodoPago,
                                @AuthenticationPrincipal EmpleadoDetails detalles,
                                RedirectAttributes redir) {

        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);

            // Obtener costo del servicio
            String sqlCosto = """
            SELECT s.costo FROM citas c
            JOIN servicios s ON c.id_servicio = s.id_servicio
            WHERE c.id_cita = ?
            """;
            double costo = 0;
            try (PreparedStatement ps = con.prepareStatement(sqlCosto)) {
                ps.setInt(1, idCita);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) costo = rs.getDouble("costo");
            }

            //Registrar en caja_movimientos
            String sqlMov = "INSERT INTO caja_movimientos (monto, tipo, categoria, descripcion, id_contador) VALUES (?, 'INGRESO', 'PAGO_CITA', ?, ?)";
            int idMov = -1;
            try (PreparedStatement ps = con.prepareStatement(sqlMov, Statement.RETURN_GENERATED_KEYS)) {
                ps.setDouble(1, costo);
                ps.setString(2, "Pago de cita #" + idCita);
                ps.setInt(3, detalles.getEmpleado().getIdPersona());
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) idMov = rs.getInt(1);
            }

            // Registrar en pagos_citas
            if (idMov != -1) {
                String sqlPago = "INSERT INTO pagos_citas (id_cita, id_movimiento, metodo_pago) VALUES (?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sqlPago)) {
                    ps.setInt(1, idCita);
                    ps.setInt(2, idMov);
                    ps.setString(3, metodoPago);
                    ps.executeUpdate();
                }
            }

            con.commit();
            redir.addFlashAttribute("exito", "✅ Cobro realizado correctamente por S/ " + costo);
        } catch (SQLException e) {
            System.err.println("Error al cobrar: " + e.getMessage());
            redir.addFlashAttribute("error", "Error al procesar el cobro.");
        }
        return "redirect:/recepcionista";
    }
    //  Búsqueda en tabla por nombre/dni
    @GetMapping("/buscar")
    public String buscarTabla(@RequestParam String texto,
                              @AuthenticationPrincipal EmpleadoDetails detalles,
                              Model model) {

        LinkedList<Paciente> pacientes = pacienteDAO.buscarPacienteTexto(texto);
        model.addAttribute("pacientes", pacientes);
        model.addAttribute("orden", "apellido");
        model.addAttribute("empleado", detalles.getEmpleado());
        return "recepcionista/panel";
    }

    // Buscar paciente por DNI (para agendar cita)
    @GetMapping("/buscar-dni")
    public String buscarPorDni(@RequestParam String dni,
                               @AuthenticationPrincipal EmpleadoDetails detalles,
                               Model model) {

        LinkedList<Paciente> todosPacientes = pacienteDAO.listarYOrdenarPacientes("dni");
        ArbolPacientes arbol = new ArbolPacientes();
        for (Paciente p : todosPacientes) arbol.insertar(p);

        Paciente pac = arbol.buscarPorDni(dni);

        model.addAttribute("pacientes", arbol.inorden());
        model.addAttribute("orden", "dni");
        model.addAttribute("empleado", detalles.getEmpleado());
        model.addAttribute("dniBuscado", dni);

        if (pac != null) {
            model.addAttribute("pacienteEncontrado", pac);
        } else {
            model.addAttribute("mensajeNoPaciente", "Paciente no encontrado. ¿Desea registrarlo?");
        }
        return "recepcionista/panel";
    }

    // Formulario registrar nuevo paciente
    @GetMapping("/registrar")
    public String formularioRegistrar(@AuthenticationPrincipal EmpleadoDetails detalles,
                                      Model model) {
        model.addAttribute("empleado", detalles.getEmpleado());
        model.addAttribute("paciente", new Paciente());
        return "recepcionista/registrar-paciente";
    }

    @PostMapping("/registrar")
    public String registrarPaciente(@RequestParam String dni,
                                    @RequestParam String nombre,
                                    @RequestParam String apellido,
                                    @RequestParam String telefono,
                                    @RequestParam String fechaNacimiento,
                                    @RequestParam String direccion,
                                    @RequestParam String genero,
                                    @RequestParam String historiaClinica,
                                    @RequestParam String tipoSeguro,
                                    @RequestParam String grupoSanguineo,
                                    RedirectAttributes redir) {

        Paciente p = new Paciente();
        p.setDni(dni);
        p.setNombre(nombre);
        p.setApellido(apellido);
        p.setTelefono(telefono);
        p.setFechaNacimiento(Date.valueOf(fechaNacimiento));
        p.setDireccion(direccion);
        p.setGenero(genero);
        p.setHistoriaClinica(historiaClinica);
        p.setTipoSeguro(tipoSeguro);
        p.setGrupoSanguineo(grupoSanguineo);

        int id = pacienteDAO.registrarPaciente(p);
        if (id != -1) {
            redir.addFlashAttribute("exito", "Paciente registrado correctamente.");
        } else {
            redir.addFlashAttribute("error", "Error al registrar el paciente.");
        }
        return "redirect:/recepcionista";
    }

    // Modificar paciente
    @PostMapping("/modificar")
    public String modificarPaciente(@RequestParam int idPersona,
                                    @RequestParam String dni,
                                    @RequestParam String nombre,
                                    @RequestParam String apellido,
                                    @RequestParam String telefono,
                                    @RequestParam String fechaNacimiento,
                                    @RequestParam String direccion,
                                    @RequestParam String genero,
                                    @RequestParam String tipoSeguro,
                                    @RequestParam String grupoSanguineo,
                                    RedirectAttributes redir) {

        Paciente p = new Paciente();
        p.setIdPersona(idPersona);
        p.setDni(dni);
        p.setNombre(nombre);
        p.setApellido(apellido);
        p.setTelefono(telefono);
        p.setFechaNacimiento(Date.valueOf(fechaNacimiento));
        p.setDireccion(direccion);
        p.setGenero(genero);
        p.setTipoSeguro(tipoSeguro);
        p.setGrupoSanguineo(grupoSanguineo);

        boolean ok = pacienteDAO.modificarPaciente(p);
        if (ok) {
            redir.addFlashAttribute("exito", "Datos del paciente actualizados.");
        } else {
            redir.addFlashAttribute("error", "Error al actualizar los datos.");
        }
        return "redirect:/recepcionista";
    }

    // Formulario agendar cita
    @GetMapping("/agendar/{idPaciente}")
    public String formularioAgendar(@PathVariable int idPaciente,
                                    @AuthenticationPrincipal EmpleadoDetails detalles,
                                    Model model) {

        Paciente pac = pacienteDAO.buscarPorId(idPaciente);
        model.addAttribute("paciente", pac);
        model.addAttribute("medicos", medicoDAO.listarMedicos());
        model.addAttribute("servicios", servicioDAO.listarServicios());
        model.addAttribute("empleado", detalles.getEmpleado());
        model.addAttribute("horas", generarHoras());
        return "recepcionista/agendar-cita";
    }

    @PostMapping("/agendar")
    public String agendarCita(@RequestParam int idPaciente,
                               @RequestParam int idMedico,
                               @RequestParam int idServicio,
                               @RequestParam String fecha,
                               @RequestParam String hora,
                               RedirectAttributes redir) {

        Date fechaSQL = Date.valueOf(fecha);
        Time horaSQL  = Time.valueOf(hora + ":00");
        Timestamp ts  = Timestamp.valueOf(fecha + " " + hora + ":00");

        // Misma validación que el btnAgendarActionPerformed original
        String resultado = medicoDAO.verificarDisponibilidadMedico(idMedico, idServicio, fechaSQL, horaSQL);
        if (!"DISPONIBLE".equals(resultado)) {
            redir.addFlashAttribute("error", resultado);
            return "redirect:/recepcionista/agendar/" + idPaciente;
        }

        Cita c = new Cita();
        c.setIdPaciente(idPaciente);
        c.setIdMedico(idMedico);
        c.setIdServicio(idServicio);
        c.setFechaCitaCom(ts);
        c.setHoraCita(horaSQL);

        boolean ok = citaDAO.registrarCita(c);
        if (ok) {
            redir.addFlashAttribute("exito", "¡Cita agendada con éxito!");
        } else {
            redir.addFlashAttribute("error", "Error al guardar la cita.");
        }
        return "redirect:/recepcionista";
    }

    // Genera las horas en formato HH:mm de 00:00 a 23:30
    private java.util.List<String> generarHoras() {
        java.util.List<String> horas = new java.util.ArrayList<>();
        for (int h = 0; h < 24; h++) {
            horas.add(String.format("%02d:00", h));
            horas.add(String.format("%02d:30", h));
        }
        return horas;
    }
    static class NodoPaciente {
        Paciente paciente;
        NodoPaciente izquierdo, derecho;
        NodoPaciente(Paciente p) { this.paciente = p; }
    }

    static class ArbolPacientes {
        NodoPaciente raiz;

        public void insertar(Paciente p) {
            raiz = insertarRec(raiz, p);
        }
        private NodoPaciente insertarRec(NodoPaciente nodo, Paciente p) {
            if (nodo == null) return new NodoPaciente(p);
            int cmp = p.getDni().compareTo(nodo.paciente.getDni());
            if (cmp < 0) nodo.izquierdo = insertarRec(nodo.izquierdo, p);
            else if (cmp > 0) nodo.derecho = insertarRec(nodo.derecho, p);
            return nodo;
        }

        public Paciente buscarPorDni(String dni) {
            return buscarRec(raiz, dni);
        }
        private Paciente buscarRec(NodoPaciente nodo, String dni) {
            if (nodo == null) return null;
            int cmp = dni.compareTo(nodo.paciente.getDni());
            if (cmp == 0) return nodo.paciente;
            if (cmp < 0)  return buscarRec(nodo.izquierdo, dni);
            return buscarRec(nodo.derecho, dni);
        }

        public LinkedList<Paciente> inorden() {
            LinkedList<Paciente> lista = new LinkedList<>();
            inordenRec(raiz, lista);
            return lista;
        }
        private void inordenRec(NodoPaciente nodo, LinkedList<Paciente> lista) {
            if (nodo == null) return;
            inordenRec(nodo.izquierdo, lista);
            lista.add(nodo.paciente);
            inordenRec(nodo.derecho, lista);
        }
    }
}
