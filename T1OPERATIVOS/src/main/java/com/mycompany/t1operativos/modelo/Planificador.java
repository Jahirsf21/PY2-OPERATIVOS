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
        boolean disponible = "FCFS".equals(algoritmo) || "SPN".equals(algoritmo) || "Prioridad".equals(algoritmo) || "HRRN".equals(algoritmo);
        return disponible;
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
        Proceso siguiente = colaListos.remove(indice);
        return siguiente;
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
                int indiceSPN = SPN();
                return indiceSPN;
            case "Prioridad":
                int indicePrioridad = Prioridad();
                return indicePrioridad;
            case "HRRN":
                int indiceHRRN = HRRN(instante);
                return indiceHRRN;
            default:
                throw new IllegalStateException("Algoritmo de planificación no disponible: " + algoritmo + ".");
        }
    }

    /**
     * Busca la menor ráfaga; empata por menor llegada y luego por menor ID.
     *
     * @return posición del proceso elegido.
     */
    private int SPN() {
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
    private int Prioridad() {
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
     * Calcula la razon de respuesta (espera + rafaga) / rafaga.
     * @param proceso BCP del proceso listo.
     * @param instante segundo actual del reloj simulado.
     * @return razon de respuesta del proceso.
     */
    private double razonRespuesta(BCP proceso, int instante) {
        int espera = instante - proceso.getLlegada();
        double razon = (double) (espera + proceso.getRafaga()) / proceso.getRafaga();
        return razon;
    }

    /**
     * Compara dos procesos por mayos razon de respuesta, empata por menor llegada y luego por menor ID.
     * @param a proceso candidato.
     * @param b proceso elegido hasta el momento.
     * @param instante segundo actual del reloj simulado.
     * @return true si a debe ir antes que b.
     */
    private boolean tienePrioridad(BCP a, BCP b, int instante) {
        double razonA = razonRespuesta(a, instante);
        double razonB = razonRespuesta(b, instante);

        if (razonA > razonB) {
            return true;
        }
        if (razonA < razonB) {
            return false;
        }
        if (a.getLlegada() < b.getLlegada()) {
            return true;
        }
        if (a.getLlegada() > b.getLlegada()) {
            return false;
        }

        if (a.getIdProceso() < b.getIdProceso()) {
            return true;
        }
        return false;
    }

    /**
     * Busca la mayor razon de respuesta, empata por menor llegada y luego por menor ID.
     * @param instante segundo actual de reloj simulado.
     * @return posicion del proceso elegido.
     */
    private int HRRN(int instante) {
        int elegido = 0;
        for (int i = 1; i < colaListos.size(); i++) {
            BCP candidato = colaListos.get(i).getBCP();
            BCP actual = colaListos.get(elegido).getBCP();
            if (tienePrioridad(candidato, actual, instante)) {
                elegido = i;
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
        boolean hay = !colaListos.isEmpty();
        return hay;
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
