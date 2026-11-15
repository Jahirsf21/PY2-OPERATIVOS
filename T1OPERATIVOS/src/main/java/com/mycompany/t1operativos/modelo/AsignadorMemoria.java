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
    private int siguientePosicion;

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
        this.siguientePosicion = memoria.getInicioUsuario();
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
     * Indica si un programa de ese tamaño puede ubicarse con el tipo de asignación.
     *
     * @param cantidad posiciones del programa.
     * @return true si alguna partición o, en asignación dinámica, la zona de usuario tiene espacio para él.
     */
    public boolean tieneEspacioPara(int cantidad) {
        if ("Fija iguales".equals(tipoAsignacion) || "Fija desiguales".equals(tipoAsignacion)) {
            for (int tamaño : tamañosParticion) {
                if (cantidad <= tamaño) {
                    return true;
                }
            }
            return false;
        }
        boolean tieneEspacio = cantidad <= memoria.getTamañoUsuario();
        return tieneEspacio;
    }

    /**
     * Busca el primer bloque libre y continuo de un tamaño dentro de un rango de la RAM.
     *
     * @param inicio primera posición del rango.
     * @param limite posición donde termina la búsqueda, sin incluirla.
     * @param cantidad posiciones requeridas.
     * @return inicio del bloque, o -1 si no hay.
     */
    private int buscarHueco(int inicio, int limite, int cantidad) {
        int libres = 0;
        for (int posicion = inicio; posicion < limite; posicion++) {
            if (memoria.leer(posicion) == null) {
                libres++;
            } else {
                libres = 0;
            }
            if (libres == cantidad) {
                int inicioHueco = posicion - cantidad + 1;
                return inicioHueco;
            }
        }
        return -1;
    }

    /**
     * Ocupa la partición libre más pequeña donde quepa el programa; empata por menor dirección.
     *
     * @param cantidad posiciones del programa.
     * @return inicio de la partición ocupada, o -1 si no hay una libre donde quepa.
     */
    private int asignarParticion(int cantidad) {
        int indiceParticion = -1;
        for (int i = 0; i < tamañosParticion.length; i++) {
            if (!particionOcupada[i] && cantidad <= tamañosParticion[i]) {
                if (indiceParticion < 0 || tamañosParticion[i] < tamañosParticion[indiceParticion]) {
                    indiceParticion = i;
                }
            }
        }
        if (indiceParticion < 0) {
            return -1;
        }
        particionOcupada[indiceParticion] = true;
        return direccionesInicio[indiceParticion];
    }

    /**
     * Busca el primer hueco suficiente desde el inicio de la zona de usuario.
     *
     * @param cantidad posiciones del programa.
     * @return inicio del hueco, o -1 si no hay.
     */
    private int firstFit(int cantidad) {
        return buscarHueco(memoria.getInicioUsuario(), memoria.getTamañoTotal(), cantidad);
    }

    /**
     * Busca el primer hueco suficiente desde la siguiente posición y, si no hay, desde el inicio de la zona de usuario.
     * La siguiente posición queda justo después del bloque asignado.
     *
     * @param cantidad posiciones del programa.
     * @return inicio del hueco, o -1 si no hay.
     */
    private int nextFit(int cantidad) {
        int inicio = buscarHueco(siguientePosicion, memoria.getTamañoTotal(), cantidad);
        if (inicio < 0) {
            inicio = buscarHueco(memoria.getInicioUsuario(), memoria.getTamañoTotal(), cantidad);
        }
        if (inicio >= 0) {
            siguientePosicion = inicio + cantidad;
            if (siguientePosicion >= memoria.getTamañoTotal()) {
                siguientePosicion = memoria.getInicioUsuario();
            }
        }
        return inicio;
    }

    /**
     * Busca el hueco suficiente más pequeño de la zona de usuario; empata por menor dirección.
     *
     * @param cantidad posiciones del programa.
     * @return inicio del hueco, o -1 si no hay.
     */
    private int bestFit(int cantidad) {
        int mejorInicio = -1;
        int mejorTamaño = 0;
        int inicioHueco = -1;
        for (int posicion = memoria.getInicioUsuario(); posicion <= memoria.getTamañoTotal(); posicion++) {
            boolean libre = posicion < memoria.getTamañoTotal() && memoria.leer(posicion) == null;
            if (libre && inicioHueco < 0) {
                inicioHueco = posicion;
            } else if (!libre && inicioHueco >= 0) {
                int tamañoHueco = posicion - inicioHueco;
                if (tamañoHueco >= cantidad && (mejorInicio < 0 || tamañoHueco < mejorTamaño)) {
                    mejorInicio = inicioHueco;
                    mejorTamaño = tamañoHueco;
                }
                inicioHueco = -1;
            }
        }
        return mejorInicio;
    }

    /**
     * Reserva espacio para un programa según el tipo de asignación.
     *
     * @param cantidad posiciones del programa.
     * @return inicio del bloque asignado, o -1 si ahora no hay espacio.
     */
    public int asignar(int cantidad) {
        switch (tipoAsignacion) {
            case "Fija iguales":
            case "Fija desiguales":
                return asignarParticion(cantidad);
            case "First Fit":
                return firstFit(cantidad);
            case "Next Fit":
                return nextFit(cantidad);
            case "Best Fit":
                return bestFit(cantidad);
            default:
                throw new IllegalStateException("Tipo de asignación no disponible: " + tipoAsignacion + ".");
        }
    }

    /**
     * Libera la partición que empieza en esa dirección.
     *
     * @param inicio dirección donde empieza el programa liberado.
     */
    public void liberar(int inicio) {
        if ("Fija iguales".equals(tipoAsignacion) || "Fija desiguales".equals(tipoAsignacion)) {
            for (int i = 0; i < direccionesInicio.length; i++) {
                if (direccionesInicio[i] == inicio) {
                    particionOcupada[i] = false;
                    return;
                }
            }
            throw new IllegalArgumentException("No hay una partición que empiece en " + inicio + ".");
        }
    }

    /**
     * Obtiene la partición a la que pertenece una posición de la RAM.
     *
     * @param posicion dirección de la RAM.
     * @return índice de la partición, o -1 si la posición no está en ninguna.
     */
    public int getParticion(int posicion) {
        for (int i = 0; i < direccionesInicio.length; i++) {
            if (posicion >= direccionesInicio[i] && posicion < direccionesInicio[i] + tamañosParticion[i]) {
                return i;
            }
        }
        return -1;
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
