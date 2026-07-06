package com.hospital.servicio;

import com.hospital.entidades.Cita;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

/**
 * Mantiene la COLA (FIFO) y la PILA (LIFO) del médico en sesión HTTP.
 * Reemplaza exactamente los campos colaAtencion y pilaHistorial
 * que vivían dentro del MedicoJFrame original.
 */
@Component
@SessionScope
public class MedicoSesion {

    // COLA (FIFO): pacientes pendientes del dia, ordenados por hora de cita
    private Queue<Cita> colaAtencion = new LinkedList<>();

    // PILA (LIFO): historial de atenciones de esta sesion (para deshacer)
    private Stack<Cita> pilaHistorial = new Stack<>();

    public Queue<Cita> getColaAtencion() { return colaAtencion; }
    public Stack<Cita> getPilaHistorial() { return pilaHistorial; }

    public void limpiarCola() { colaAtencion.clear(); }

    public void cargarCola(LinkedList<Cita> pendientes) {
        colaAtencion.clear();
        for (Cita c : pendientes) {
            colaAtencion.offer(c); // ENQUEUE
        }
    }
}
