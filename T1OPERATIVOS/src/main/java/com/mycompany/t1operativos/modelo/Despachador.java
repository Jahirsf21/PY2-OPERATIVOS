package com.mycompany.t1operativos.modelo;

/**
 * Administra la CPU y los cambios de contexto.
 *
 * @author deislher sánchez funez
 */
public class Despachador {
    private CPU cpu;
    private Proceso actual;

    /**
     * Construye el despachador con la CPU compartida.
     *
     * @param cpu CPU de la simulación.
     */
    public Despachador(CPU cpu) {
        if (cpu == null) {
            throw new IllegalArgumentException("La CPU no puede ser nula.");
        }
        this.cpu = cpu;
        this.actual = null;
    }

    /**
     * Restaura y asigna CPU al proceso listo.
     *
     * @param proceso proceso seleccionado.
     * @param instante segundo del reloj simulado.
     */
    public void despachar(Proceso proceso, int instante) {
        if (proceso == null) {
            throw new IllegalArgumentException("El proceso no puede ser nulo.");
        }
        BCP bcp = proceso.getBCP();
        if (actual != null || !"LISTO".equals(bcp.getEstado())) {
            throw new IllegalStateException("La CPU debe estar libre y el proceso listo.");
        }
        bcp.restaurarContexto(cpu);
        bcp.setCpuActual(1);
        bcp.setEstadoEjecutando(instante);
        actual = proceso;
    }

    /**
     * Guarda el contexto y retira al proceso actual.
     *
     * @return proceso que dejó la CPU.
     */
    public Proceso retirarActual() {
        if (actual == null) {
            throw new IllegalStateException("No hay un proceso actual para retirar.");
        }
        BCP bcp = actual.getBCP();
        if (!"TERMINADO".equals(bcp.getEstado())) {
            throw new IllegalStateException("El proceso debe estar terminado para retirar.");
        }
        Proceso retirado = actual;
        retirado.getBCP().guardarContexto(cpu);
        retirado.getBCP().setCpuActual(-1);
        cpu.reiniciar();
        actual = null;
        return retirado;
    }

    /**
     * Expropia al proceso actual entre instrucciones: guarda su contexto, lo
     * devuelve a LISTO y libera la CPU. No finaliza el proceso, no libera su
     * memoria ni cambia su primer inicio.
     *
     * @return proceso expropiado.
     */
    public Proceso expropiarActual() {
        if (actual == null) {
            throw new IllegalStateException("No hay un proceso actual para expropiar.");
        }
        BCP bcp = actual.getBCP();
        if (!"EJECUTANDO".equals(bcp.getEstado())) {
            throw new IllegalStateException("Solo se expropia un proceso en ejecución.");
        }
        Proceso expropiado = actual;
        bcp.guardarContexto(cpu);
        bcp.setCpuActual(-1);
        bcp.setEstadoListo();
        cpu.reiniciar();
        actual = null;
        return expropiado;
    }

    /**
     * Obtiene el proceso que tiene CPU.
     *
     * @return proceso actual, o null.
     */
    public Proceso getActual() {
        return actual;
    }

    /**
     * Obtiene la CPU compartida.
     *
     * @return CPU de la simulación.
     */
    public CPU getCPU() {
        return cpu;
    }
}
