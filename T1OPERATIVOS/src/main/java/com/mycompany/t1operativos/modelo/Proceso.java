package com.mycompany.t1operativos.modelo;

/**
 * Reúne el contexto y las ubicaciones de un proceso.
 *
 * @author Deislher Sánchez Funez
 * @author Natalia Granados Rosales
 */
public class Proceso {
    private BCP bcp;
    private int posicionIndice;
    private int posicionBCP;
    private int nivel;
    private int quantumUsado;
    private int tiempoEspera;
    private int prioridad;
    private boolean aceptado;

    /**
     * Construye un trabajo sin memoria asignada.
     *
     * @param bcp contexto del proceso.
     * @param posicionIndice entrada del archivo en disco.
     */
    public Proceso(BCP bcp, int posicionIndice) {
        if (bcp == null || posicionIndice < 0) {
            throw new IllegalArgumentException("El proceso necesita un BCP y una entrada de índice válidos.");
        }
        this.bcp = bcp;
        this.posicionIndice = posicionIndice;
        this.posicionBCP = -1;
        this.nivel = 0;
        this.quantumUsado = 0;
        this.tiempoEspera = 0;
        this.prioridad = 0;
        this.aceptado = false;
    }

    /**
     * Obtiene el contexto del proceso.
     *
     * @return el mismo BCP asociado.
     */
    public BCP getBCP() {
        return bcp;
    }

    /**
     * Obtiene la entrada del archivo.
     *
     * @return posición del índice.
     */
    public int getPosicionIndice() {
        return posicionIndice;
    }

    /**
     * Obtiene la dirección del BCP en kernel.
     *
     * @return dirección, o -1 sin bloque en kernel.
     */
    public int getPosicionBCP() {
        return posicionBCP;
    }

    /**
     * Registra la dirección del BCP.
     *
     * @param posicion primera posición en kernel.
     */
    public void asignarPosicionBCP(int posicion) {
        if (posicion < 0) {
            throw new IllegalArgumentException("La posición del BCP no puede ser negativa.");
        }
        if (!"NUEVO".equals(bcp.getEstado()) || posicionBCP != -1) {
            throw new IllegalStateException("El proceso ya tiene BCP en RAM o dejó de ser nuevo.");
        }
        posicionBCP = posicion;
    }

    /**
     * Obtiene el nivel de MLFQ del proceso.
     *
     * @return nivel, 0 es el más alto.
     */
    public int getNivel() {
        return nivel;
    }

    /**
     * Cambia el nivel de MLFQ del proceso.
     *
     * @param nivel nivel no negativo.
     */
    public void setNivel(int nivel) {
        if (nivel < 0) {
            throw new IllegalArgumentException("El nivel no puede ser negativo.");
        }
        this.nivel = nivel;
    }

    /**
     * Obtiene los segundos del quantum de su nivel usados antes de ser interrumpido.
     *
     * @return segundos usados.
     */
    public int getQuantumUsado() {
        return quantumUsado;
    }

    /**
     * Guarda los segundos del quantum de su nivel ya usados.
     *
     * @param quantumUsado segundos no negativos.
     */
    public void setQuantumUsado(int quantumUsado) {
        if (quantumUsado < 0) {
            throw new IllegalArgumentException("El quantum usado no puede ser negativo.");
        }
        this.quantumUsado = quantumUsado;
    }

    /**
     * Obtiene los segundos seguidos que lleva listo sin ejecutarse.
     *
     * @return segundos de espera.
     */
    public int getTiempoEspera() {
        return tiempoEspera;
    }

    /**
     * Cambia los segundos seguidos que lleva listo sin ejecutarse.
     *
     * @param tiempoEspera segundos no negativos.
     */
    public void setTiempoEspera(int tiempoEspera) {
        if (tiempoEspera < 0) {
            throw new IllegalArgumentException("El tiempo de espera no puede ser negativo.");
        }
        this.tiempoEspera = tiempoEspera;
    }

    /**
     * Obtiene la prioridad de SRR del proceso.
     *
     * @return prioridad acumulada.
     */
    public int getPrioridad() {
        return prioridad;
    }

    /**
     * Cambia la prioridad de SRR del proceso.
     *
     * @param prioridad prioridad no negativa.
     */
    public void setPrioridad(int prioridad) {
        if (prioridad < 0) {
            throw new IllegalArgumentException("La prioridad no puede ser negativa.");
        }
        this.prioridad = prioridad;
    }

    /**
     * Indica si el proceso está en la lista de aceptados de SRR.
     *
     * @return true si está aceptado.
     */
    public boolean estaAceptado() {
        return aceptado;
    }

    /**
     * Marca si el proceso pasa a la lista de aceptados de SRR.
     *
     * @param aceptado true si pasa a aceptado.
     */
    public void setAceptado(boolean aceptado) {
        this.aceptado = aceptado;
    }

    /**
     * Retira la dirección del BCP terminado.
     */
    public void liberarPosicionBCP() {
        if (!"TERMINADO".equals(bcp.getEstado()) || posicionBCP < 0) {
            throw new IllegalStateException("Solo se libera la posición de un BCP terminado y con memoria asignada.");
        }
        posicionBCP = -1;
    }
}
