package com.mycompany.t1operativos.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Mantiene los procesos listos y elige el siguiente según el algoritmo
 * configurado.
 *
 * @author deislher sánchez funez
 */
public class Planificador {
    private List<Proceso> colaListos;
    private String algoritmo;

    /**
     * Construye una cola vacía de procesos listos para CPU.
     *
     * @param algoritmo nombre del algoritmo de planificación.
     */
    public Planificador(String algoritmo) {
        if (!esAlgoritmoDisponible(algoritmo)) {
            throw new IllegalArgumentException("Algoritmo de planificación no disponible: " + algoritmo + ".");
        }
        colaListos = new ArrayList<>();
        this.algoritmo = algoritmo;
    }

    /**
     * Comprueba si un algoritmo está implementado.
     *
     * @param algoritmo nombre del algoritmo.
     * @return true si puede seleccionarse.
     */
    public static boolean esAlgoritmoDisponible(String algoritmo) {
        return "FCFS".equals(algoritmo) || "SPN".equals(algoritmo) || "Prioridad".equals(algoritmo) || "HRRN".equals(algoritmo);
    }

    /**
     * Obtiene el algoritmo configurado.
     *
     * @return nombre del algoritmo.
     */
    public String getAlgoritmo() {
        return algoritmo;
    }

    /**
     * Añade un proceso al final de listos.
     *
     * @param proceso proceso listo y cargado en RAM.
     */
    public void agregarListo(Proceso proceso) {
        if (proceso == null) {
            throw new IllegalArgumentException("El proceso no puede ser nulo.");
        }
        BCP bcp = proceso.getBCP();
        if (!"LISTO".equals(bcp.getEstado())) {
            throw new IllegalStateException("Solo se agregan procesos listos.");
        }
        for (Proceso listo : colaListos) {
            if (listo.getBCP().getIdProceso() == bcp.getIdProceso()) {
                throw new IllegalStateException("El proceso ya está en la cola de listos.");
            }
        }
        colaListos.add(proceso);
    }

    /**
     * Retira el proceso que elige el algoritmo.
     *
     * @param instante segundo actual del reloj simulado.
     * @return proceso seleccionado, o null.
     */
    public Proceso obtenerSiguiente(int instante) {
        if (colaListos.isEmpty()) {
            return null;
        }
        int indice = elegirIndice(instante);
        BCP bcp = colaListos.get(indice).getBCP();
        if (!"LISTO".equals(bcp.getEstado())) {
            throw new IllegalStateException("El proceso seleccionado debe estar listo.");
        }
        return colaListos.remove(indice);
    }

    /**
     * Elige la posición del siguiente proceso en la cola de listos.
     *
     * @param instante segundo actual del reloj simulado.
     * @return posición dentro de la cola.
     */
    private int elegirIndice(int instante) {
        switch (algoritmo) {
            case "FCFS":
                return 0;
            case "SPN":
                return elegirMenorRafaga();
            case "Prioridad":
                return elegirMayorPrioridad();
            case "HRRN":
                return elegirMayorRazonRespuesta(instante);
            default:
                throw new IllegalStateException("Algoritmo de planificación no disponible: " + algoritmo + ".");
        }
    }

    /**
     * Busca la menor ráfaga; empata por menor llegada y luego por menor ID.
     *
     * @return posición del proceso elegido.
     */
    private int elegirMenorRafaga() {
        int elegido = 0;
        for (int i = 1; i < colaListos.size(); i++) {
            BCP candidato = colaListos.get(i).getBCP();
            BCP actual = colaListos.get(elegido).getBCP();
            if (candidato.getRafaga() < actual.getRafaga()) {
                elegido = i;
            } else if (candidato.getRafaga() == actual.getRafaga()) {
                if (candidato.getLlegada() < actual.getLlegada()) {
                    elegido = i;
                } else if (candidato.getLlegada() == actual.getLlegada() && candidato.getIdProceso() < actual.getIdProceso()) {
                    elegido = i;
                }
            }
        }
        return elegido;
    }

    /**
     * Busca el mayor número de prioridad; empata por menor llegada y luego por menor ID.
     *
     * @return posición del proceso elegido.
     */
    private int elegirMayorPrioridad() {
        int elegido = 0;
        for (int i = 1; i < colaListos.size(); i++) {
            BCP candidato = colaListos.get(i).getBCP();
            BCP actual = colaListos.get(elegido).getBCP();
            if (candidato.getPrioridad() > actual.getPrioridad()) {
                elegido = i;
            } else if (candidato.getPrioridad() == actual.getPrioridad()) {
                if (candidato.getLlegada() < actual.getLlegada()) {
                    elegido = i;
                } else if (candidato.getLlegada() == actual.getLlegada() && candidato.getIdProceso() < actual.getIdProceso()) {
                    elegido = i;
                }
            }
        }
        return elegido;
    }

    /**
     * Busca la mayor razón de respuesta (espera + ráfaga) / ráfaga, con
     * espera = instante - llegada; empata por menor llegada y luego por menor ID.
     * Las razones se comparan multiplicando en cruz, sin división entera.
     *
     * @param instante segundo actual del reloj simulado.
     * @return posición del proceso elegido.
     */
    private int elegirMayorRazonRespuesta(int instante) {
        int elegido = 0;
        for (int i = 1; i < colaListos.size(); i++) {
            BCP candidato = colaListos.get(i).getBCP();
            BCP actual = colaListos.get(elegido).getBCP();
            int numeradorCandidato = instante - candidato.getLlegada() + candidato.getRafaga();
            int numeradorActual = instante - actual.getLlegada() + actual.getRafaga();
            int izquierda = numeradorCandidato * actual.getRafaga();
            int derecha = numeradorActual * candidato.getRafaga();
            if (izquierda > derecha) {
                elegido = i;
            } else if (izquierda == derecha) {
                if (candidato.getLlegada() < actual.getLlegada()) {
                    elegido = i;
                } else if (candidato.getLlegada() == actual.getLlegada() && candidato.getIdProceso() < actual.getIdProceso()) {
                    elegido = i;
                }
            }
        }
        return elegido;
    }

    /**
     * Comprueba si hay procesos listos.
     *
     * @return true si la cola no está vacía.
     */
    public boolean hayProcesosListos() {
        return !colaListos.isEmpty();
    }

    /**
     * Obtiene la cola de procesos listos para CPU.
     *
     * @return cola de procesos listos.
     */
    public List<Proceso> getListos() {
        return colaListos;
    }

    /**
     * Vacía la cola de listos.
     */
    public void limpiar() {
        colaListos.clear();
    }
}
