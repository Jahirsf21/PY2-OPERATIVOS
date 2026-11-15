package com.mycompany.t1operativos.modelo;

/**
 * Asigna y libera la zona de usuario de la RAM según el tipo de asignación elegido.
 *
 * @author Deislher Sánchez Funez
 * @author Natalia Granados Rosales
 */
public class AsignadorMemoria {
    private Memoria memoria;
    private String tipoAsignacion;

    /**
     * Construye el asignador de la zona de usuario.
     *
     * @param memoria RAM compartida.
     * @param tipoAsignacion Fija iguales, Fija desiguales, First Fit, Next Fit o Best Fit.
     * @param cantidadParticiones cantidad de particiones para Fija iguales.
     * @param tamañosParticion tamaño de cada partición para Fija desiguales.
     */
    public AsignadorMemoria(Memoria memoria, String tipoAsignacion, int cantidadParticiones, int[] tamañosParticion) {
        if (memoria == null) {
            throw new IllegalArgumentException("La memoria no puede ser nula.");
        }
        if (!esTipoAsignacionDisponible(tipoAsignacion)) {
            throw new IllegalArgumentException("Tipo de asignación no disponible: " + tipoAsignacion + ".");
        }
        this.memoria = memoria;
        this.tipoAsignacion = tipoAsignacion;
    }

    /**
     * Obtiene el tipo de asignación configurado.
     *
     * @return nombre del tipo de asignación.
     */
    public String getTipoAsignacion() {
        return tipoAsignacion;
    }

    /**
     * Comprueba si un tipo de asignación está disponible.
     *
     * @param tipo nombre del tipo de asignación.
     * @return true si puede usarse.
     */
    private boolean esTipoAsignacionDisponible(String tipo) {
        boolean disponible = "Fija iguales".equals(tipo) || "Fija desiguales".equals(tipo) || "First Fit".equals(tipo) || "Next Fit".equals(tipo) || "Best Fit".equals(tipo);
        return disponible;
    }
}