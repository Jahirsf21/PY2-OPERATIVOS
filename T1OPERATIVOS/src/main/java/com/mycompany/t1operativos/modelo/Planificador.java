package com.mycompany.t1operativos.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mantiene los procesos listos y elige el siguiente según el algoritmo
 * configurado.
 *
 * @author Deislher Sánchez Funez
 * @author Natalia Granados Rosales
 */
public class Planificador {
    private List<Proceso> colaListos;
    private String algoritmo;
    private int quantum;
    private Random sorteo;
    private String ultimoSorteo;

    /**
     * Construye una cola vacía de procesos listos para CPU.
     *
     * @param algoritmo nombre del algoritmo de planificación.
     * @param quantum segundos por turno en RR.
     */
    public Planificador(String algoritmo, int quantum) {
        if (!esAlgoritmoDisponible(algoritmo)) {
            throw new IllegalArgumentException("Algoritmo de planificación no disponible: " + algoritmo + ".");
        }
        if (quantum < 1) {
            throw new IllegalArgumentException("El quantum debe ser un entero mayor o igual a 1.");
        }
        colaListos = new ArrayList<>();
        this.algoritmo = algoritmo;
        this.quantum = quantum;
        this.sorteo = new Random();
        this.ultimoSorteo = null;
    }

    /**
     * Comprueba si un algoritmo está implementado.
     *
     * @param algoritmo nombre del algoritmo.
     * @return true si puede seleccionarse.
     */
    public static boolean esAlgoritmoDisponible(String algoritmo) {
        boolean disponible = "FCFS".equals(algoritmo) || "SPN".equals(algoritmo) || "Prioridad".equals(algoritmo) || "HRRN".equals(algoritmo) || "SRT".equals(algoritmo) || "RR".equals(algoritmo) || "Lottery".equals(algoritmo) || "GuaranteedScheduling".equals(algoritmo);
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
     * Obtiene el quantum configurado.
     *
     * @return segundos por turno.
     */
    public int getQuantum() {
        return quantum;
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
                return SPN();
            case "Prioridad":
                return Prioridad();
            case "HRRN":
                return HRRN(instante);
            case "SRT":
                return SRT();
            case "RR":
                return 0;
            case "Lottery":
                return Lottery();
            case "GuaranteedScheduling":
                return GuaranteedScheduling(instante);
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
     * Calcula la razon de respuesta (transcurrido + rafaga) / rafaga.
     * @param proceso BCP del proceso listo.
     * @param instante segundo actual del reloj simulado.
     * @return razon de respuesta del proceso.
     */
    private double calcularRazonHRRN(BCP proceso, int instante) {
        int transcurrido = instante - proceso.getLlegada();
        double razon = (double) (transcurrido + proceso.getRafaga()) / proceso.getRafaga();
        return razon;
    }

    /**
     * Compara dos procesos por mayor razon de respuesta, empata por menor llegada y luego por menor ID.
     * @param a proceso candidato.
     * @param b proceso elegido hasta el momento.
     * @param instante segundo actual del reloj simulado.
     * @return true si a debe ir antes que b.
     */
    private boolean tienePrioridadHRRN(BCP a, BCP b, int instante) {
        double razonA = calcularRazonHRRN(a, instante);
        double razonB = calcularRazonHRRN(b, instante);

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
            if (tienePrioridadHRRN(candidato, actual, instante)) {
                elegido = i;
            }
        }
        return elegido;
    }

    /**
     * Calcula la razón entre la CPU usada y la que le corresponde a un proceso
     *
     * @param proceso BCP del proceso.
     * @param instante segundo actual del reloj simulado.
     * @param participantes procesos que comparten la CPU.
     * @return razón del proceso, o 0 si acaba de llegar.
     */
    public double calcularRazonGS(BCP proceso, int instante, int participantes) {
        int transcurrido = instante - proceso.getLlegada();
        if (transcurrido == 0) {
            return 0;
        }
        double razon = (double) (proceso.getTiempoEmpleadoSegundos() * participantes) / transcurrido;
        return razon;
    }

    /**
     * Compara dos procesos por menor razón de uso; empata por menor llegada y luego por menor ID.
     *
     * @param a proceso candidato.
     * @param b proceso elegido hasta el momento.
     * @param instante segundo actual del reloj simulado.
     * @param participantes procesos que comparten la CPU.
     * @return true si a debe ir antes que b.
     */
    private boolean tienePrioridadGS(BCP a, BCP b, int instante, int participantes) {
        double razonA = calcularRazonGS(a, instante, participantes);
        double razonB = calcularRazonGS(b, instante, participantes);

        if (razonA < razonB) {
            return true;
        }
        if (razonA > razonB) {
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
     * Busca la menor razón de uso; empata por menor llegada y luego por menor ID.
     *
     * @param instante segundo actual del reloj simulado.
     * @return posición del proceso elegido.
     */
    private int GuaranteedScheduling(int instante) {
        int participantes = colaListos.size();
        int elegido = 0;
        for (int i = 1; i < colaListos.size(); i++) {
            BCP candidato = colaListos.get(i).getBCP();
            BCP actual = colaListos.get(elegido).getBCP();
            if (tienePrioridadGS(candidato, actual, instante, participantes)) {
                elegido = i;
            }
        }
        return elegido;
    }

    /**
     * Busca el menor tiempo restante; empata por menor llegada y luego por menor ID.
     *
     * @return posición del proceso elegido.
     */
    private int SRT() {
        int elegido = 0;
        for (int i = 1; i < colaListos.size(); i++) {
            BCP candidato = colaListos.get(i).getBCP();
            BCP actual = colaListos.get(elegido).getBCP();
            if (candidato.getRestante() < actual.getRestante()) {
                elegido = i;
            } else if (candidato.getRestante() == actual.getRestante()) {
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
     * Sortea un boleto entre los listos; gana el proceso dueño de ese boleto.
     *
     * @return posición del proceso elegido.
     */
    private int Lottery() {
        int totalBoletos = 0;
        for (Proceso listo : colaListos) {
            totalBoletos = totalBoletos + listo.getBCP().getBoletos();
        }
        int boleto = sorteo.nextInt(totalBoletos) + 1;
        int acumulado = 0;
        int elegido = 0;
        for (int i = 0; i < colaListos.size(); i++) {
            acumulado = acumulado + colaListos.get(i).getBCP().getBoletos();
            if (boleto <= acumulado) {
                elegido = i;
                break;
            }
        }
        ultimoSorteo = "boleto " + boleto + " de " + totalBoletos + " → P" + colaListos.get(elegido).getBCP().getIdProceso();
        return elegido;
    }

    /**
     * Obtiene el resultado del último sorteo de Lottery.
     *
     * @return boleto, totalBoletos y ganador, o null si no hubo sorteo.
     */
    public String getUltimoSorteo() {
        return ultimoSorteo;
    }

    /**
     * Indica si el proceso actual debe dejar la CPU.
     * SRT expropia: cuando algún listo tiene un restante estrictamente menor que el del actual.
     * GuaranteedScheduling expropia: cuando algún listo tiene una razón de uso estrictamente menor que la del actual.
     * El empate no expropia.
     *
     * @param actual proceso que tiene la CPU.
     * @param instante segundo actual del reloj simulado.
     * @return true si debe expropiarse.
     */
    public boolean debeExpropiar(Proceso actual, int instante) {
        boolean expropiar = false;
        if ("SRT".equals(algoritmo) && actual != null) {
            int restanteActual = actual.getBCP().getRestante();
            for (Proceso listo : colaListos) {
                if (listo.getBCP().getRestante() < restanteActual) {
                    expropiar = true;
                }
            }
        }
        if ("GuaranteedScheduling".equals(algoritmo) && actual != null) {
            int participantes = colaListos.size() + 1;
            double razonActual = calcularRazonGS(actual.getBCP(), instante, participantes);
            for (Proceso listo : colaListos) {
                if (calcularRazonGS(listo.getBCP(), instante, participantes) < razonActual) {
                    expropiar = true;
                }
            }
        }
        return expropiar;
    }

    /**
     * Indica si el proceso actual debe dejar la CPU por quantum.
     * RR y Lottery expropian cuando su turno alcanza el quantum configurado.
     *
     * @param segundosTurno segundos ejecutados desde su despacho.
     * @return true si debe expropiarse.
     */
    public boolean agotoQuantum(int segundosTurno) {
        boolean agotado = ("RR".equals(algoritmo) || "Lottery".equals(algoritmo)) && segundosTurno >= quantum;
        return agotado;
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
