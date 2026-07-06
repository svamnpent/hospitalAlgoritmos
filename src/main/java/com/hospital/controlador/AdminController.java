package com.hospital.controlador;

import com.hospital.config.EmpleadoDetails;
import com.hospital.dao.ReporteDAO;
import com.hospital.dao.UsuarioDAO;
import com.hospital.entidades.ReporteCitas;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UsuarioDAO usuarioDAO;
    @Autowired
    private ReporteDAO reporteDAO;

    @GetMapping
    public String panel(@AuthenticationPrincipal EmpleadoDetails detalles,
                        Model model,
                        @RequestParam(required = false) Integer year) {

        if (year == null) year = LocalDate.now().getYear();

        ReporteCitas stats = reporteDAO.obtenerEstadisticasGenerales();
        model.addAttribute("stats", stats);
        model.addAttribute("empleado", detalles.getEmpleado());

        model.addAttribute("citasPorEspecialidad", reporteDAO.obtenerCitasPorEspecialidad());
        model.addAttribute("citasPorMedico", reporteDAO.obtenerCitasPorMedico());
        model.addAttribute("citasPorEstado", reporteDAO.obtenerCitasPorEstado());
        model.addAttribute("citasPorMes", reporteDAO.obtenerCitasPorMes(year));
        model.addAttribute("yearSeleccionado", year);
        model.addAttribute("years", List.of(2024, 2025, 2026));

        // Árbol personal completo
        LinkedList<Object[]> todoPersonal = usuarioDAO.listarPersonalParaArbol();
        ArbolPersonal arbol = new ArbolPersonal();
        for (Object[] fila : todoPersonal) arbol.insertar(fila);
        model.addAttribute("personal", arbol.inorden());

        return "admin/panel";
    }

    // 🔍 Búsqueda en tiempo real (AJAX)
    @GetMapping("/buscar-ajax")
    @ResponseBody
    public List<Object[]> buscarAjax(@RequestParam String dni) {
        LinkedList<Object[]> todoPersonal = usuarioDAO.listarPersonalParaArbol();
        ArbolPersonal arbol = new ArbolPersonal();
        for (Object[] fila : todoPersonal) arbol.insertar(fila);

        if (dni == null || dni.trim().isEmpty()) {
            return arbol.inorden();
        }

        // Filtrar mientras escribe (contenga el texto)
        return arbol.inorden().stream()
                .filter(f -> f[0].toString().contains(dni.trim()))
                .collect(Collectors.toList());
    }

    @GetMapping("/buscar")
    public String buscar(@RequestParam String dni,
                         @AuthenticationPrincipal EmpleadoDetails detalles,
                         Model model) {

        LinkedList<Object[]> todoPersonal = usuarioDAO.listarPersonalParaArbol();
        ArbolPersonal arbol = new ArbolPersonal();
        for (Object[] fila : todoPersonal) arbol.insertar(fila);

        Object[] encontrado = arbol.buscarPorDni(dni);

        model.addAttribute("personal", arbol.inorden());
        model.addAttribute("empleado", detalles.getEmpleado());
        model.addAttribute("dniBuscado", dni);

        if (encontrado != null) {
            model.addAttribute("personalEncontrado", encontrado);
        } else {
            model.addAttribute("mensajeNo", "No se encontró personal con DNI: " + dni);
        }
        return "admin/panel";
    }

    static class NodoPersonal {
        Object[] fila;
        NodoPersonal izquierdo, derecho;
        NodoPersonal(Object[] fila) { this.fila = fila; }
    }

    static class ArbolPersonal {
        NodoPersonal raiz;

        public void insertar(Object[] fila) {
            raiz = insertarRec(raiz, fila);
        }
        private NodoPersonal insertarRec(NodoPersonal nodo, Object[] fila) {
            if (nodo == null) return new NodoPersonal(fila);
            int cmp = fila[0].toString().compareTo(nodo.fila[0].toString());
            if (cmp < 0) nodo.izquierdo = insertarRec(nodo.izquierdo, fila);
            else if (cmp > 0) nodo.derecho = insertarRec(nodo.derecho, fila);
            return nodo;
        }

        public Object[] buscarPorDni(String dni) {
            return buscarRec(raiz, dni);
        }
        private Object[] buscarRec(NodoPersonal nodo, String dni) {
            if (nodo == null) return null;
            int cmp = dni.compareTo(nodo.fila[0].toString());
            if (cmp == 0) return nodo.fila;
            if (cmp < 0) return buscarRec(nodo.izquierdo, dni);
            return buscarRec(nodo.derecho, dni);
        }

        public LinkedList<Object[]> inorden() {
            LinkedList<Object[]> lista = new LinkedList<>();
            inordenRec(raiz, lista);
            return lista;
        }
        private void inordenRec(NodoPersonal nodo, LinkedList<Object[]> lista) {
            if (nodo == null) return;
            inordenRec(nodo.izquierdo, lista);
            lista.add(nodo.fila);
            inordenRec(nodo.derecho, lista);
        }
    }
}