package com.hospital.controlador;

import com.hospital.config.EmpleadoDetails;
import com.hospital.dao.CitaDAO;
import com.hospital.dao.PacienteDAO;
import com.hospital.entidades.Cita;
import com.hospital.servicio.MedicoSesion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;

@Controller
@RequestMapping("/medico")
public class MedicoController {

    @Autowired private PacienteDAO pacienteDAO;
    @Autowired private CitaDAO citaDAO;
    @Autowired private MedicoSesion medicoSesion;

    //  Panel principal del médico
    @GetMapping
    public String panel(@AuthenticationPrincipal EmpleadoDetails detalles, Model model) {
        int idMedico = detalles.getEmpleado().getIdPersona();

        // ===== LOGS DE DEPURACION =====
        System.out.println("=== MEDICO LOGUEADO ID: " + idMedico + " ===");
        LocalDateTime ahoraLima = LocalDateTime.now(ZoneId.of("America/Lima"));
        System.out.println("Hora actual en Lima: " + ahoraLima.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // Si la cola está vacía la cargamos (primera vez o al actualizar)
        if (medicoSesion.getColaAtencion().isEmpty()) {
            LinkedList<Cita> pendientes = citaDAO.listarCitasPendientesHoy(idMedico);
            System.out.println("Citas encontradas para hoy: " + pendientes.size());
            for (Cita c : pendientes) {
                System.out.println("  Cita ID: " + c.getIdCita() +
                        " - Fecha: " + c.getFechaCitaCom() +
                        " - Paciente: " + c.getNombrePaciente() +
                        " - Servicio: " + c.getNombreServicio());
            }
            medicoSesion.cargarCola(pendientes);
        } else {
            System.out.println("Cola ya cargada con " + medicoSesion.getColaAtencion().size() + " citas");
        }

        model.addAttribute("empleado", detalles.getEmpleado());
        model.addAttribute("pacientes", pacienteDAO.listarPacientesPorMedico(idMedico));
        model.addAttribute("cola", medicoSesion.getColaAtencion());

        // Pila, mostramos de tope a base
        LinkedList<Cita> historial = new LinkedList<>(medicoSesion.getPilaHistorial());
        java.util.Collections.reverse(historial);
        model.addAttribute("historial", historial);

        return "medico/panel";
    }

    //  Actualizar cola (botón Actualizar)
    @PostMapping("/actualizar")
    public String actualizar(@AuthenticationPrincipal EmpleadoDetails detalles) {
        int idMedico = detalles.getEmpleado().getIdPersona();
        System.out.println("=== ACTUALIZANDO COLA PARA MEDICO ID: " + idMedico + " ===");

        LinkedList<Cita> pendientes = citaDAO.listarCitasPendientesHoy(idMedico);
        System.out.println("Citas encontradas al actualizar: " + pendientes.size());
        for (Cita c : pendientes) {
            System.out.println("  Cita ID: " + c.getIdCita() + " - Fecha: " + c.getFechaCitaCom());
        }

        medicoSesion.cargarCola(pendientes);
        medicoSesion.getPilaHistorial().clear();
        return "redirect:/medico";
    }

    // Atender siguiente (DEQUEUE + PUSH)
    @PostMapping("/atender")
    public String atenderSiguiente(RedirectAttributes redir) {
        if (medicoSesion.getColaAtencion().isEmpty()) {
            redir.addFlashAttribute("aviso", "No hay pacientes en espera.");
            return "redirect:/medico";
        }

        Cita siguiente = medicoSesion.getColaAtencion().poll(); // DEQUEUE
        boolean ok = citaDAO.actualizarEstadoCita(siguiente.getIdCita(), "ATENDIDA");

        if (!ok) {
            ((LinkedList<Cita>) medicoSesion.getColaAtencion()).addFirst(siguiente);
            redir.addFlashAttribute("error", "No se pudo actualizar el estado de la cita.");
        } else {
            medicoSesion.getPilaHistorial().push(siguiente); // PUSH
            redir.addFlashAttribute("exito",
                    "Atendiendo a: " + siguiente.getNombrePaciente() + " — " + siguiente.getNombreServicio());
        }
        return "redirect:/medico";
    }

    // Deshacer última atención (POP)
    @PostMapping("/deshacer")
    public String deshacer(RedirectAttributes redir) {
        if (medicoSesion.getPilaHistorial().isEmpty()) {
            redir.addFlashAttribute("aviso", "No hay atenciones para deshacer.");
            return "redirect:/medico";
        }

        Cita ultima = medicoSesion.getPilaHistorial().pop(); // POP
        boolean ok = citaDAO.actualizarEstadoCita(ultima.getIdCita(), "PENDIENTE");

        if (!ok) {
            medicoSesion.getPilaHistorial().push(ultima);
            redir.addFlashAttribute("error", "No se pudo revertir el estado de la cita.");
        } else {
            ((LinkedList<Cita>) medicoSesion.getColaAtencion()).addFirst(ultima);
            redir.addFlashAttribute("exito", "Atención deshecha. Paciente devuelto al frente de la cola.");
        }
        return "redirect:/medico";
    }
}
