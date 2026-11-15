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
    private int[] direccionesInicio;
    private int[] tamañosParticion;
    private boolean[] particionOcupada;

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
        this.direccionesInicio = new int[0];
        this.tamañosParticion = new int[0];
        this.particionOcupada = new boolean[0];
        if ("Fija iguales".equals(tipoAsignacion)) {
            crearParticionesIguales(cantidadParticiones);
        } else if ("Fija desiguales".equals(tipoAsignacion)) {
            crearParticionesDesiguales(tamañosParticion);
        }
    }

    /**
     * Divide la zona de usuario en particiones del mismo tamaño, sin sobrante.
     *
     * @param cantidad cantidad de particiones, al menos 2.
     */
    private void crearParticionesIguales(int cantidad) {
        int zonaUsuario = memoria.getTamañoUsuario();
        if (cantidad < 2 || cantidad > zonaUsuario) {
            throw new IllegalArgumentException("La cantidad de particiones debe estar entre 2 y " + zonaUsuario + ".");
        }
        if (zonaUsuario % cantidad != 0) {
            throw new IllegalArgumentException("La zona de usuario tiene " + zonaUsuario + " posiciones y no se divide exacto en " + cantidad + " particiones.");
        }
        int tamaño = zonaUsuario / cantidad;
        direccionesInicio = new int[cantidad];
        tamañosParticion = new int[cantidad];
        particionOcupada = new boolean[cantidad];
        for (int i = 0; i < cantidad; i++) {
            direccionesInicio[i] = memoria.getInicioUsuario() + i * tamaño;
            tamañosParticion[i] = tamaño;
            particionOcupada[i] = false;
        }
    }

    /**
     * Ubica una detrás de otra las particiones de los tamaños dados, que ocupan toda la zona de usuario.
     *
     * @param tamaños tamaño de cada partición, al menos 2 particiones.
     */
    private void crearParticionesDesiguales(int[] tamaños) {
        if (tamaños == null || tamaños.length < 2) {
            throw new IllegalArgumentException("Debe haber al menos 2 particiones desiguales.");
        }
        int suma = 0;
        for (int tamaño : tamaños) {
            if (tamaño < 1) {
                throw new IllegalArgumentException("El tamaño de cada partición debe ser un entero mayor o igual a 1.");
            }
            suma = suma + tamaño;
        }
        int zonaUsuario = memoria.getTamañoUsuario();
        if (suma != zonaUsuario) {
            throw new IllegalArgumentException("Las particiones suman " + suma + " y deben sumar exacto la zona de usuario de " + zonaUsuario + " posiciones.");
        }
        direccionesInicio = new int[tamaños.length];
        tamañosParticion = tamaños;
        particionOcupada = new boolean[tamaños.length];
        int inicio = memoria.getInicioUsuario();
        for (int i = 0; i < tamaños.length; i++) {
            direccionesInicio[i] = inicio;
            particionOcupada[i] = false;
            inicio = inicio + tamaños[i];
        }
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
     * Obtiene la dirección de inicio de cada partición.
     *
     * @return direcciones de inicio; vacío en asignación dinámica.
     */
    public int[] getDireccionesInicio() {
        return direccionesInicio;
    }

    /**
     * Obtiene el tamaño de cada partición.
     *
     * @return tamaños de partición; vacío en asignación dinámica.
     */
    public int[] getTamañosParticion() {
        return tamañosParticion;
    }

    /**
     * Indica si una partición está ocupada por un proceso.
     *
     * @param particion índice de la partición.
     * @return true si está ocupada.
     */
    public boolean estaOcupada(int particion) {
        return particionOcupada[particion];
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