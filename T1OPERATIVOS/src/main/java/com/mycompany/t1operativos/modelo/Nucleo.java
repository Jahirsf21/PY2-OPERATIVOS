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
}
