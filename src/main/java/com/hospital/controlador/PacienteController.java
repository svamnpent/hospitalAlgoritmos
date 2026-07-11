package com.hospital.controlador;

import com.hospital.dao.PacienteDAO;
import com.hospital.dao.CitaDAO;
import com.hospital.entidades.Paciente;
import com.hospital.entidades.Cita;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedList;

@Controller
public class PacienteController {

    @Autowired
    private PacienteDAO pacienteDAO;

    @Autowired
    private CitaDAO citaDAO;

    // Mostrar panel de eleccion
    @GetMapping("/elige")
    public String elegir() {
        return "auth/eleccion";
    }

    // Mostrar login de personal (redirige al login normal)
    @GetMapping("/login-personal")
    public String loginPersonal() {
        return "redirect:/login";
    }

    // Formulario para paciente (solo DNI)
    @GetMapping("/login-paciente")
    public String loginPaciente() {
        return "auth/login-paciente";
    }

    // Procesar login de paciente
    @PostMapping("/paciente/ingresar")
    public String ingresarPaciente(@RequestParam String dni,
                                   Model model,
                                   RedirectAttributes redir) {

        System.out.println("=== BUSCANDO PACIENTE CON DNI: " + dni + " ===");

        // Buscar paciente por DNI
        Paciente paciente = pacienteDAO.buscarPorDni(dni);

        if (paciente == null) {
            System.out.println(" Paciente NO encontrado con DNI: " + dni);
            model.addAttribute("error", "Paciente no encontrado");
            model.addAttribute("dni", dni);
            model.addAttribute("mostrarRegistro", true);
            return "auth/login-paciente";
        }

        System.out.println(" Paciente encontrado: " + paciente.getNombre() + " " + paciente.getApellido());
        System.out.println("   ID: " + paciente.getIdPersona());
        System.out.println("   DNI: " + paciente.getDni());

        // Obtener citas del paciente
        System.out.println(" Buscando citas pendientes para paciente ID: " + paciente.getIdPersona());
        LinkedList<Cita> citasPendientes = citaDAO.listarCitasPendientesPorPaciente(paciente.getIdPersona());
        System.out.println("   Citas pendientes encontradas: " + citasPendientes.size());

        System.out.println(" Buscando citas atendidas para paciente ID: " + paciente.getIdPersona());
        LinkedList<Cita> citasAtendidas = citaDAO.listarCitasAtendidasPorPaciente(paciente.getIdPersona());
        System.out.println("   Citas atendidas encontradas: " + citasAtendidas.size());

        // Mostrar citas en consola para depuración
        for (Cita c : citasPendientes) {
            System.out.println("    Cita pendiente: ID=" + c.getIdCita() +
                    ", Fecha=" + c.getFechaCitaCom() +
                    ", Servicio=" + c.getNombreServicio());
        }

        model.addAttribute("paciente", paciente);
        model.addAttribute("citasPendientes", citasPendientes);
        model.addAttribute("citasAtendidas", citasAtendidas);
        model.addAttribute("tieneCitas", !citasPendientes.isEmpty());

        return "paciente/dashboard";
    }
}