package com.mycompany.t1operativos.modelo;

/**
 * Agrupa el planificador, el despachador y los contadores de una CPU del simulador.
 *
 * @author Deislher Sánchez Funez
 * @author Natalia Granados Rosales
 */
public class Nucleo {
    private int numero;
    private Planificador planificador;
    private Despachador despachador;
    private int quantumUsado;
    private int segundosOcio;

    /**
     * Construye una CPU del simulador.
     *
     * @param numero número de la CPU, de 1 a 5.
     * @param planificador planificador de esta CPU.
     * @param despachador despachador con la CPU.
     */
    public Nucleo(int numero, Planificador planificador, Despachador despachador) {
        if (numero < 1 || numero > 5) {
            throw new IllegalArgumentException("El número de CPU debe estar entre 1 y 5.");
        }
        if (planificador == null || despachador == null) {
            throw new IllegalArgumentException("El planificador y el despachador no pueden ser nulos.");
        }
        this.numero = numero;
        this.planificador = planificador;
        this.despachador = despachador;
        this.quantumUsado = 0;
        this.segundosOcio = 0;
    }

    /**
     * Obtiene el número de la CPU.
     *
     * @return número de 1 a 5.
     */
    public int getNumero() {
        return numero;
    }

    /**
     * Obtiene el planificador de esta CPU.
     *
     * @return planificador con su cola de listos.
     */
    public Planificador getPlanificador() {
        return planificador;
    }

    /**
     * Obtiene el despachador de esta CPU.
     *
     * @return despachador con la CPU y el proceso actual.
     */
    public Despachador getDespachador() {
        return despachador;
    }

    /**
     * Obtiene los segundos usados del quantum en curso.
     *
     * @return segundos del turno actual.
     */
    public int getQuantumUsado() {
        return quantumUsado;
    }

    /**
     * Cambia los segundos usados del quantum en curso.
     *
     * @param quantumUsado segundos, no negativos.
     */
    public void setQuantumUsado(int quantumUsado) {
        if (quantumUsado < 0) {
            throw new IllegalArgumentException("El quantum usado no puede ser negativo.");
        }
        this.quantumUsado = quantumUsado;
    }

    /**
     * Suma un segundo al quantum en curso.
     */
    public void aumentarQuantumUsado() {
        quantumUsado++;
    }

    /**
     * Obtiene el ocio de esta CPU.
     *
     * @return segundos sin proceso en ejecución.
     */
    public int getSegundosOcio() {
        return segundosOcio;
    }

    /**
     * Suma un segundo de ocio a esta CPU.
     */
    public void aumentarOcio() {
        segundosOcio++;
    }
}
