package com.mycompany.t1operativos.modelo;

import java.util.List;

/**
 * Guarda el resultado de una simulación terminada.
 *
 * @author Deislher Sánchez Funez
 * @author Natalia Granados Rosales
 */
public class ResultadoSimulacion {
    private int numero;
    private List<Nucleo> nucleos;
    private AsignadorMemoria asignador;
    private List<Proceso> procesos;
    private int tiempoFinal;

    /**
     * Construye el resultado de una simulación terminada.
     *
     * @param numero número de la simulación, desde 1.
     * @param nucleos CPU usadas en la simulación.
     * @param asignador asignador de memoria usado en la simulación.
     * @param procesos procesos de la simulación.
     * @param tiempoFinal reloj al terminar la simulación.
     */
    public ResultadoSimulacion(int numero, List<Nucleo> nucleos, AsignadorMemoria asignador, List<Proceso> procesos, int tiempoFinal) {
        if (numero < 1) {
            throw new IllegalArgumentException("El número de simulación debe ser al menos 1.");
        }
        if (nucleos == null || nucleos.isEmpty() || asignador == null || procesos == null) {
            throw new IllegalArgumentException("Las CPU, el asignador y los procesos no pueden ser nulos.");
        }
        this.numero = numero;
        this.nucleos = nucleos;
        this.asignador = asignador;
        this.procesos = procesos;
        this.tiempoFinal = tiempoFinal;
    }

    /**
     * Obtiene el número de la simulación.
     *
     * @return número desde 1.
     */
    public int getNumero() {
        return numero;
    }

    /**
     * Obtiene las CPU usadas en la simulación.
     *
     * @return núcleos en orden de número.
     */
    public List<Nucleo> getNucleos() {
        return nucleos;
    }

    /**
     * Obtiene el planificador de la CPU 1, con el algoritmo y los parámetros de la simulación.
     *
     * @return planificador de la CPU 1.
     */
    public Planificador getPlanificador() {
        return nucleos.get(0).getPlanificador();
    }

    /**
     * Obtiene el asignador de memoria usado en la simulación.
     *
     * @return asignador de la simulación.
     */
    public AsignadorMemoria getAsignador() {
        return asignador;
    }

    /**
     * Obtiene los procesos de la simulación.
     *
     * @return procesos de la simulación.
     */
    public List<Proceso> getProcesos() {
        return procesos;
    }

    /**
     * Obtiene el reloj al terminar la simulación.
     *
     * @return segundo final.
     */
    public int getTiempoFinal() {
        return tiempoFinal;
    }

    /**
     * Obtiene el ocio de todas las CPU en la simulación.
     *
     * @return suma de los segundos sin proceso en ejecución.
     */
    public int getSegundosOcio() {
        int ocio = 0;
        for (Nucleo nucleo : nucleos) {
            ocio = ocio + nucleo.getSegundosOcio();
        }
        return ocio;
    }

    /**
     * Calcula el tiempo de retorno de un proceso: final menos llegada.
     *
     * @param proceso proceso de la simulación.
     * @return tiempo de retorno, o -1 si no terminó.
     */
    public int calcularRetorno(Proceso proceso) {
        BCP bcp = proceso.getBCP();
        if (bcp.getTiempoFinal() < 0) {
            return -1;
        }
        int retorno = bcp.getTiempoFinal() - bcp.getLlegada();
        return retorno;
    }

    /**
     * Calcula la razón entre el tiempo de retorno y el servicio de un proceso.
     *
     * @param proceso proceso de la simulación.
     * @return Tr/Ts, o -1 si no terminó o no tuvo servicio.
     */
    public double calcularRazonRetorno(Proceso proceso) {
        int retorno = calcularRetorno(proceso);
        int servicio = proceso.getBCP().getTiempoEmpleadoSegundos();
        if (retorno < 0 || servicio == 0) {
            return -1;
        }
        double razon = (double) retorno / servicio;
        return razon;
    }

    /**
     * Calcula el tiempo de retorno promedio de los procesos terminados con servicio.
     *
     * @return promedio de Tr, o -1 si no hay procesos que promediar.
     */
    public double calcularPromedioRetorno() {
        int suma = 0;
        int cantidad = 0;
        for (Proceso proceso : procesos) {
            if (calcularRazonRetorno(proceso) >= 0) {
                suma = suma + calcularRetorno(proceso);
                cantidad++;
            }
        }
        if (cantidad == 0) {
            return -1;
        }
        double promedio = (double) suma / cantidad;
        return promedio;
    }

    /**
     * Calcula la razón Tr/Ts promedio de los procesos terminados con servicio.
     *
     * @return promedio de Tr/Ts, o -1 si no hay procesos que promediar.
     */
    public double calcularPromedioRazon() {
        double suma = 0;
        int cantidad = 0;
        for (Proceso proceso : procesos) {
            double razon = calcularRazonRetorno(proceso);
            if (razon >= 0) {
                suma = suma + razon;
                cantidad++;
            }
        }
        if (cantidad == 0) {
            return -1;
        }
        double promedio = suma / cantidad;
        return promedio;
    }
}
