package com.hospital.controlador;

import com.hospital.config.EmpleadoDetails;
import com.hospital.dao.RRHHDAO;
import com.hospital.entidades.Empleado;
import com.hospital.entidades.ReportePersonal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/rrhh")
public class RRHHController {

    @Autowired
    private RRHHDAO rrhhDAO;

    @GetMapping
    public String panel(@AuthenticationPrincipal EmpleadoDetails detalles,
                        Model model,
                        @RequestParam(required = false) String estado,
                        @RequestParam(required = false) Integer idRol) {

        ReportePersonal resumen = rrhhDAO.obtenerResumenPersonal();
        model.addAttribute("resumen", resumen);
        model.addAttribute("empleado", detalles.getEmpleado());

        model.addAttribute("empleados", rrhhDAO.listarEmpleados(estado, idRol));
        model.addAttribute("distribucion", rrhhDAO.obtenerDistribucionPorRol());
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("idRolFiltro", idRol);

        model.addAttribute("roles", List.of(
                new Object[]{1, "ADMIN"},
                new Object[]{2, "RECEPCIONISTA"},
                new Object[]{3, "DOCTOR"},
                new Object[]{4, "CONTADOR"},
                new Object[]{5, "RRHH"}
        ));

        model.addAttribute("diasDescanso", List.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"));

        return "rrhh/panel";
    }

    // ── CONTRATAR NUEVO EMPLEADO ─────────────────────────────────
    @PostMapping("/contratar")
    public String contratar(@RequestParam String dni,
                            @RequestParam String nombre,
                            @RequestParam String apellido,
                            @RequestParam String telefono,
                            @RequestParam String fechaNacimiento,
                            @RequestParam String direccion,
                            @RequestParam String genero,
                            @RequestParam int idRol,
                            @RequestParam double sueldoBase,
                            @RequestParam String fechaContratacion,
                            @RequestParam String diaDescanso,
                            @RequestParam String horaEntrada,
                            @RequestParam String horaSalida,
                            @RequestParam String username,
                            @RequestParam String password,
                            RedirectAttributes redir) {

        boolean ok = rrhhDAO.contratarEmpleado(dni, nombre, apellido, telefono,
                Date.valueOf(fechaNacimiento), direccion, genero,
                idRol, sueldoBase, Date.valueOf(fechaContratacion),
                diaDescanso, Time.valueOf(horaEntrada + ":00"), Time.valueOf(horaSalida + ":00"),
                username, password);

        if (ok) {
            redir.addFlashAttribute("exito", "✅ Empleado contratado exitosamente.");
        } else {
            redir.addFlashAttribute("error", "❌ Error al contratar empleado.");
        }
        return "redirect:/rrhh";
    }

    @PostMapping("/cambiar-estado")
    public String cambiarEstado(@RequestParam int idEmpleado,
                                @RequestParam String estado,
                                RedirectAttributes redir) {
        boolean ok = rrhhDAO.actualizarEstadoContrato(idEmpleado, estado);
        if (ok) {
            redir.addFlashAttribute("exito", "Estado del empleado actualizado.");
        } else {
            redir.addFlashAttribute("error", "Error al actualizar el estado.");
        }
        return "redirect:/rrhh";
    }

    @PostMapping("/actualizar-horario")
    public String actualizarHorario(@RequestParam int idEmpleado,
                                    @RequestParam String diaDescanso,
                                    @RequestParam String horaEntrada,
                                    @RequestParam String horaSalida,
                                    RedirectAttributes redir) {
        Time he = Time.valueOf(horaEntrada + ":00");
        Time hs = Time.valueOf(horaSalida + ":00");
        boolean ok = rrhhDAO.actualizarHorario(idEmpleado, diaDescanso, he, hs);
        if (ok) {
            redir.addFlashAttribute("exito", "Horario actualizado correctamente.");
        } else {
            redir.addFlashAttribute("error", "Error al actualizar el horario.");
        }
        return "redirect:/rrhh";
    }
}