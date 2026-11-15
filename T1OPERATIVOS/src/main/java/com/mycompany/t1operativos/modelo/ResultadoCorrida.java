package com.mycompany.t1operativos.modelo;

import java.util.List;

/**
 * Guarda el resultado de una corrida terminada.
 *
 * @author Deislher Sánchez Funez
 * @author Natalia Granados Rosales
 */
public class ResultadoCorrida {
    private int numero;
    private Planificador planificador;
    private AsignadorMemoria asignador;
    private List<Proceso> procesos;
    private int tiempoFinal;
    private int segundosOcio;

    /**
     * Construye el resultado de una corrida terminada.
     *
     * @param numero número de la corrida, desde 1.
     * @param planificador planificador usado en la corrida.
     * @param asignador asignador de memoria usado en la corrida.
     * @param procesos procesos de la corrida.
     * @param tiempoFinal reloj al terminar la corrida.
     * @param segundosOcio segundos sin proceso en ejecución.
     */
    public ResultadoCorrida(int numero, Planificador planificador, AsignadorMemoria asignador, List<Proceso> procesos, int tiempoFinal, int segundosOcio) {
        if (numero < 1) {
            throw new IllegalArgumentException("El número de corrida debe ser al menos 1.");
        }
        if (planificador == null || asignador == null || procesos == null) {
            throw new IllegalArgumentException("El planificador, el asignador y los procesos no pueden ser nulos.");
        }
        this.numero = numero;
        this.planificador = planificador;
        this.asignador = asignador;
        this.procesos = procesos;
        this.tiempoFinal = tiempoFinal;
        this.segundosOcio = segundosOcio;
    }

    /**
     * Obtiene el número de la corrida.
     *
     * @return número desde 1.
     */
    public int getNumero() {
        return numero;
    }

    /**
     * Obtiene el planificador usado en la corrida.
     *
     * @return planificador de la corrida.
     */
    public Planificador getPlanificador() {
        return planificador;
    }

    /**
     * Obtiene el asignador de memoria usado en la corrida.
     *
     * @return asignador de la corrida.
     */
    public AsignadorMemoria getAsignador() {
        return asignador;
    }

    /**
     * Obtiene los procesos de la corrida.
     *
     * @return procesos de la corrida.
     */
    public List<Proceso> getProcesos() {
        return procesos;
    }

    /**
     * Obtiene el reloj al terminar la corrida.
     *
     * @return segundo final.
     */
    public int getTiempoFinal() {
        return tiempoFinal;
    }

    /**
     * Obtiene el ocio de la CPU en la corrida.
     *
     * @return segundos sin proceso en ejecución.
     */
    public int getSegundosOcio() {
        return segundosOcio;
    }
}
