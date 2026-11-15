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
}
