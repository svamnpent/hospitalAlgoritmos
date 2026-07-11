package com.hospital.controlador;

import com.hospital.config.EmpleadoDetails;
import com.hospital.dao.CajaDAO;
import com.hospital.entidades.CajaMovimiento;
import com.hospital.entidades.ReporteFinanciero;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/contador")
public class ContadorController {

    @Autowired
    private CajaDAO cajaDAO;

    @GetMapping
    public String panel(@AuthenticationPrincipal EmpleadoDetails detalles,
                        Model model,
                        @RequestParam(required = false) Integer year,
                        @RequestParam(required = false) String filtroTipo,
                        @RequestParam(required = false) String filtroCategoria,
                        @RequestParam(required = false) String fechaInicio,
                        @RequestParam(required = false) String fechaFin,
                        @RequestParam(required = false) Integer revertirId) {

        if (year == null) year = LocalDate.now().getYear();

        ReporteFinanciero resumen = cajaDAO.obtenerResumenFinanciero();
        model.addAttribute("resumen", resumen);
        model.addAttribute("empleado", detalles.getEmpleado());

        model.addAttribute("ingresosPorMes", cajaDAO.obtenerIngresosPorMes(year));
        model.addAttribute("egresosPorCategoria", cajaDAO.obtenerEgresosPorCategoria());
        model.addAttribute("yearSeleccionado", year);
        model.addAttribute("years", List.of(2024, 2025, 2026));

        boolean soloIngresos = "INGRESO".equals(filtroTipo);
        boolean soloEgresos = "EGRESO".equals(filtroTipo);
        List<CajaMovimiento> movimientos = cajaDAO.listarMovimientos(soloIngresos, soloEgresos,
                filtroCategoria, fechaInicio, fechaFin);
        model.addAttribute("movimientos", movimientos);
        model.addAttribute("filtroTipo", filtroTipo);
        model.addAttribute("filtroCategoria", filtroCategoria);
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);

        // Si se seleccionó un movimiento para revertir
        if (revertirId != null) {
            CajaMovimiento seleccionado = movimientos.stream()
                    .filter(m -> m.getIdMovimiento() == revertirId)
                    .findFirst()
                    .orElse(null);
            model.addAttribute("movimientoSeleccionado", seleccionado);
            model.addAttribute("mostrarBotonRevertir", true);
        } else {
            model.addAttribute("mostrarBotonRevertir", false);
        }

        // Lista de empleados para pago
        model.addAttribute("empleados", cajaDAO.listarEmpleadosActivos());
        model.addAttribute("roles", List.of(
                new Object[]{1, "ADMIN"},
                new Object[]{2, "RECEPCIONISTA"},
                new Object[]{3, "DOCTOR"},
                new Object[]{4, "CONTADOR"},
                new Object[]{5, "RRHH"}
        ));

        // Lista de citas atendidas sin pagar
        model.addAttribute("citasPendientesPago", cajaDAO.listarCitasAtendidasSinPagar());

        return "contador/panel";
    }

    // Registrar pago de cita
    @PostMapping("/cobrar-cita")
    public String cobrarCita(@RequestParam int idCita,
                             @RequestParam BigDecimal monto,
                             @RequestParam String metodoPago,
                             @AuthenticationPrincipal EmpleadoDetails detalles,
                             RedirectAttributes redir) {

        boolean ok = cajaDAO.registrarPagoCita(idCita, monto, metodoPago, detalles.getEmpleado().getIdPersona());
        if (ok) {
            redir.addFlashAttribute("exito", "Pago de cita registrado: S/ " + monto);
        } else {
            redir.addFlashAttribute("error", "Error al registrar el pago.");
        }
        return "redirect:/contador";
    }

    // Registrar pago a personal
    @PostMapping("/pagar-personal")
    public String pagarPersonal(@RequestParam int idEmpleado,
                                @RequestParam BigDecimal monto,
                                @RequestParam String nombreEmpleado,
                                @AuthenticationPrincipal EmpleadoDetails detalles,
                                RedirectAttributes redir) {

        boolean ok = cajaDAO.registrarPagoPersonal(idEmpleado, monto, nombreEmpleado, detalles.getEmpleado().getIdPersona());
        if (ok) {
            redir.addFlashAttribute("exito", "Pago a " + nombreEmpleado + " registrado: S/ " + monto);
        } else {
            redir.addFlashAttribute("error", "Error al registrar el pago.");
        }
        return "redirect:/contador";
    }

    // Revertir un movimiento específico
    @PostMapping("/revertir")
    public String revertirMovimiento(@RequestParam int idMovimiento,
                                     @AuthenticationPrincipal EmpleadoDetails detalles,
                                     RedirectAttributes redir) {

        String resultado = cajaDAO.revertirMovimiento(idMovimiento, detalles.getEmpleado().getIdPersona());
        if (resultado.startsWith("OK")) {
            redir.addFlashAttribute("exito", resultado.substring(3));
        } else {
            redir.addFlashAttribute("error", resultado);
        }
        return "redirect:/contador";
    }

    // Obtener empleados por rol (AJAX)
    @GetMapping("/empleados-por-rol")
    @ResponseBody
    public List<Object[]> empleadosPorRol(@RequestParam int idRol) {
        return cajaDAO.listarEmpleadosPorRol(idRol);
    }

    // Obtener monto de cita (AJAX)
    @GetMapping("/monto-cita")
    @ResponseBody
    public BigDecimal montoCita(@RequestParam int idCita) {
        return cajaDAO.obtenerMontoCita(idCita);
    }
}