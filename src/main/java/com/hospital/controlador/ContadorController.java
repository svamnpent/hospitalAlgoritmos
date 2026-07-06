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
                        @RequestParam(required = false) String fechaFin) {

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
        model.addAttribute("movimientos", cajaDAO.listarMovimientos(soloIngresos, soloEgresos,
                filtroCategoria, fechaInicio, fechaFin));
        model.addAttribute("filtroTipo", filtroTipo);
        model.addAttribute("filtroCategoria", filtroCategoria);
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);

        return "contador/panel";
    }

    @PostMapping("/registrar-egreso")
    public String registrarEgreso(@RequestParam BigDecimal monto,
                                  @RequestParam String categoria,
                                  @RequestParam String descripcion,
                                  @AuthenticationPrincipal EmpleadoDetails detalles,
                                  RedirectAttributes redir) {

        CajaMovimiento m = new CajaMovimiento();
        m.setMonto(monto);
        m.setTipo("EGRESO");
        m.setCategoria(categoria);
        m.setDescripcion(descripcion);
        m.setIdContador(detalles.getEmpleado().getIdPersona());

        boolean ok = cajaDAO.registrarMovimiento(m);
        if (ok) {
            redir.addFlashAttribute("exito", "Egreso registrado correctamente.");
        } else {
            redir.addFlashAttribute("error", "Error al registrar el egreso.");
        }
        return "redirect:/contador";
    }
}