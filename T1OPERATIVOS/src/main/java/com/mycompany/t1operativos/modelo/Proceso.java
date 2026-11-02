package com.mycompany.t1operativos.modelo;

/**
 * Reúne el contexto y las ubicaciones de un proceso.
 *
 * @author deislher sánchez funez
 */
public class Proceso {
    private BCP bcp;
    private int posicionIndice;
    private int posicionBCP;

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
     * Retira la dirección del BCP terminado.
     */
    public void liberarPosicionBCP() {
        if (!"TERMINADO".equals(bcp.getEstado()) || posicionBCP < 0) {
            throw new IllegalStateException("Solo se libera la posición de un BCP terminado y con memoria asignada.");
        }
        posicionBCP = -1;
    }
}
