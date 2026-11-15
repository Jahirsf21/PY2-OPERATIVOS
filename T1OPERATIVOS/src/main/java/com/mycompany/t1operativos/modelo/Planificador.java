package com.mycompany.t1operativos.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mantiene los procesos listos y elige el siguiente según el algoritmo
 * configurado.
 *
 * @author Deislher Sánchez Funez
 * @author Natalia Granados Rosales
 */
public class Planificador {
    private List<Proceso> colaListos;
    private String algoritmo;
    private int quantum;
    private Random sorteo;
    private String ultimoSorteo;
    private int[] quantumsNivel;
    private int limiteEspera;

    /**
     * Construye una cola vacía de procesos listos para CPU.
     *
     * @param algoritmo nombre del algoritmo de planificación.
     * @param quantum segundos por turno en RR.
     * @param quantumsNivel quantum de cada nivel de MLFQ, de 2 a 5 niveles.
     * @param limiteEspera segundos de espera para subir de nivel en MLFQ.
     */
    public Planificador(String algoritmo, int quantum, int[] quantumsNivel, int limiteEspera) {
        if (!esAlgoritmoDisponible(algoritmo)) {
            throw new IllegalArgumentException("Algoritmo de planificación no disponible: " + algoritmo + ".");
        }
        if (quantum < 1) {
            throw new IllegalArgumentException("El quantum debe ser un entero mayor o igual a 1.");
        }
        if (quantumsNivel == null || quantumsNivel.length < 2 || quantumsNivel.length > 5) {
            throw new IllegalArgumentException("Multilevel Feedback Queue necesita de 2 a 5 niveles.");
        }
        for (int quantumNivel : quantumsNivel) {
            if (quantumNivel < 1) {
                throw new IllegalArgumentException("El quantum de cada nivel debe ser un entero mayor o igual a 1.");
            }
        }
        if (limiteEspera < 1) {
            throw new IllegalArgumentException("El límite de espera debe ser un entero mayor o igual a 1.");
        }
        colaListos = new ArrayList<>();
        this.algoritmo = algoritmo;
        this.quantum = quantum;
        this.sorteo = new Random();
        this.ultimoSorteo = null;
        this.quantumsNivel = quantumsNivel;
        this.limiteEspera = limiteEspera;
    }

    /**
     * Comprueba si un algoritmo está implementado.
     *
     * @param algoritmo nombre del algoritmo.
     * @return true si puede seleccionarse.
     */
    public static boolean esAlgoritmoDisponible(String algoritmo) {
        boolean disponible = "FCFS".equals(algoritmo) || "SPN".equals(algoritmo) || "Prioridad".equals(algoritmo) || "HRRN".equals(algoritmo) || "SRT".equals(algoritmo) || "RR".equals(algoritmo) || "Lottery".equals(algoritmo) || "GuaranteedScheduling".equals(algoritmo) || "MLFQ".equals(algoritmo);
        return disponible;
    }

    /**
     * Obtiene el algoritmo configurado.
     *
     * @return nombre del algoritmo.
     */
    public String getAlgoritmo() {
        return algoritmo;
    }

    /**
     * Obtiene el quantum configurado.
     *
     * @return segundos por turno.
     */
    public int getQuantum() {
        return quantum;
    }

    /**
     * Obtiene el quantum de cada nivel de MLFQ.
     *
     * @return quantums por nivel.
     */
    public int[] getQuantumsNivel() {
        return quantumsNivel;
    }

    /**
     * Obtiene los segundos de espera para subir de nivel en MLFQ.
     *
     * @return límite de espera.
     */
    public int getLimiteEspera() {
        return limiteEspera;
    }

    /**
     * Añade un proceso al final de listos.
     *
     * @param proceso proceso listo y cargado en RAM.
     */
    public void agregarListo(Proceso proceso) {
        if (proceso == null) {
            throw new IllegalArgumentException("El proceso no puede ser nulo.");
        }
        BCP bcp = proceso.getBCP();
        if (!"LISTO".equals(bcp.getEstado())) {
            throw new IllegalStateException("Solo se agregan procesos listos.");
        }
        for (Proceso listo : colaListos) {
            if (listo.getBCP().getIdProceso() == bcp.getIdProceso()) {
                throw new IllegalStateException("El proceso ya está en la cola de listos.");
            }
        }
        colaListos.add(proceso);
    }

    /**
     * Devuelve a listos un proceso interrumpido; en MLFQ queda al frente con el quantum que llevaba.
     *
     * @param proceso proceso interrumpido.
     * @param quantumUsado segundos del quantum usados en su turno.
     */
    public void reinsertarInterrumpido(Proceso proceso, int quantumUsado) {
        agregarListo(proceso);
        if ("MLFQ".equals(algoritmo)) {
            proceso.setQuantumUsado(quantumUsado);
            colaListos.remove(colaListos.size() - 1);
            colaListos.add(0, proceso);
        }
    }

    /**
     * Retira el proceso que elige el algoritmo.
     *
     * @param instante segundo actual del reloj simulado.
     * @return proceso seleccionado, o null.
     */
    public Proceso obtenerSiguiente(int instante) {
        if (colaListos.isEmpty()) {
            return null;
        }
        int indice = elegirIndice(instante);
        BCP bcp = colaListos.get(indice).getBCP();
        if (!"LISTO".equals(bcp.getEstado())) {
            throw new IllegalStateException("El proceso seleccionado debe estar listo.");
        }
        return colaListos.remove(indice);
    }

    /**
     * Elige la posición del siguiente proceso en la cola de listos.
     *
     * @param instante segundo actual del reloj simulado.
     * @return posición dentro de la cola.
     */
    private int elegirIndice(int instante) {
        switch (algoritmo) {
            case "FCFS":
                return 0;
            case "SPN":
                return SPN();
            case "Prioridad":
                return Prioridad();
            case "HRRN":
                return HRRN(instante);
            case "SRT":
                return SRT();
            case "RR":
                return 0;
            case "Lottery":
                return Lottery();
            case "GuaranteedScheduling":
                return GuaranteedScheduling(instante);
            case "MLFQ":
                return MLFQ();
            default:
                throw new IllegalStateException("Algoritmo de planificación no disponible: " + algoritmo + ".");
        }
    }

    /**
     * Busca la menor ráfaga; empata por menor llegada y luego por menor ID.
     *
     * @return posición del proceso elegido.
     */
    private int SPN() {
        int elegido = 0;
        for (int i = 1; i < colaListos.size(); i++) {
            BCP candidato = colaListos.get(i).getBCP();
            BCP actual = colaListos.get(elegido).getBCP();
            if (candidato.getRafaga() < actual.getRafaga()) {
                elegido = i;
            } else if (candidato.getRafaga() == actual.getRafaga()) {
                if (candidato.getLlegada() < actual.getLlegada()) {
                    elegido = i;
                } else if (candidato.getLlegada() == actual.getLlegada() && candidato.getIdProceso() < actual.getIdProceso()) {
                    elegido = i;
                }
            }
        }
        return elegido;
    }

    /**
     * Busca el mayor número de prioridad; empata por menor llegada y luego por menor ID.
     *
     * @return posición del proceso elegido.
     */
    private int Prioridad() {
        int elegido = 0;
        for (int i = 1; i < colaListos.size(); i++) {
            BCP candidato = colaListos.get(i).getBCP();
            BCP actual = colaListos.get(elegido).getBCP();
            if (candidato.getPrioridad() > actual.getPrioridad()) {
                elegido = i;
            } else if (candidato.getPrioridad() == actual.getPrioridad()) {
                if (candidato.getLlegada() < actual.getLlegada()) {
                    elegido = i;
                } else if (candidato.getLlegada() == actual.getLlegada() && candidato.getIdProceso() < actual.getIdProceso()) {
                    elegido = i;
                }
            }
        }
        return elegido;
    }

    /**
     * Calcula la razon de respuesta (transcurrido + rafaga) / rafaga.
     * @param proceso BCP del proceso listo.
     * @param instante segundo actual del reloj simulado.
     * @return razon de respuesta del proceso.
     */
    private double calcularRazonHRRN(BCP proceso, int instante) {
        int transcurrido = instante - proceso.getLlegada();
        double razon = (double) (transcurrido + proceso.getRafaga()) / proceso.getRafaga();
        return razon;
    }

    /**
     * Compara dos procesos por mayor razon de respuesta, empata por menor llegada y luego por menor ID.
     * @param a proceso candidato.
     * @param b proceso elegido hasta el momento.
     * @param instante segundo actual del reloj simulado.
     * @return true si a debe ir antes que b.
     */
    private boolean tienePrioridadHRRN(BCP a, BCP b, int instante) {
        double razonA = calcularRazonHRRN(a, instante);
        double razonB = calcularRazonHRRN(b, instante);

        if (razonA > razonB) {
            return true;
        }
        if (razonA < razonB) {
            return false;
        }
        if (a.getLlegada() < b.getLlegada()) {
            return true;
        }
        if (a.getLlegada() > b.getLlegada()) {
            return false;
        }

        if (a.getIdProceso() < b.getIdProceso()) {
            return true;
        }
        return false;
    }

    /**
     * Busca la mayor razon de respuesta, empata por menor llegada y luego por menor ID.
     * @param instante segundo actual de reloj simulado.
     * @return posicion del proceso elegido.
     */
    private int HRRN(int instante) {
        int elegido = 0;
        for (int i = 1; i < colaListos.size(); i++) {
            BCP candidato = colaListos.get(i).getBCP();
            BCP actual = colaListos.get(elegido).getBCP();
            if (tienePrioridadHRRN(candidato, actual, instante)) {
                elegido = i;
            }
        }
        return elegido;
    }

    /**
     * Calcula la razón entre la CPU usada y la que le corresponde a un proceso
     *
     * @param proceso BCP del proceso.
     * @param instante segundo actual del reloj simulado.
     * @param participantes procesos que comparten la CPU.
     * @return razón del proceso, o 0 si acaba de llegar.
     */
    public double calcularRazonGS(BCP proceso, int instante, int participantes) {
        int transcurrido = instante - proceso.getLlegada();
        if (transcurrido == 0) {
            return 0;
        }
        double razon = (double) (proceso.getTiempoEmpleadoSegundos() * participantes) / transcurrido;
        return razon;
    }

    /**
     * Compara dos procesos por menor razón de uso; empata por menor llegada y luego por menor ID.
     *
     * @param a proceso candidato.
     * @param b proceso elegido hasta el momento.
     * @param instante segundo actual del reloj simulado.
     * @param participantes procesos que comparten la CPU.
     * @return true si a debe ir antes que b.
     */
    private boolean tienePrioridadGS(BCP a, BCP b, int instante, int participantes) {
        double razonA = calcularRazonGS(a, instante, participantes);
        double razonB = calcularRazonGS(b, instante, participantes);

        if (razonA < razonB) {
            return true;
        }
        if (razonA > razonB) {
            return false;
        }
        if (a.getLlegada() < b.getLlegada()) {
            return true;
        }
        if (a.getLlegada() > b.getLlegada()) {
            return false;
        }

        if (a.getIdProceso() < b.getIdProceso()) {
            return true;
        }
        return false;
    }

    /**
     * Busca la menor razón de uso; empata por menor llegada y luego por menor ID.
     *
     * @param instante segundo actual del reloj simulado.
     * @return posición del proceso elegido.
     */
    private int GuaranteedScheduling(int instante) {
        int participantes = colaListos.size();
        int elegido = 0;
        for (int i = 1; i < colaListos.size(); i++) {
            BCP candidato = colaListos.get(i).getBCP();
            BCP actual = colaListos.get(elegido).getBCP();
            if (tienePrioridadGS(candidato, actual, instante, participantes)) {
                elegido = i;
            }
        }
        return elegido;
    }

    /**
     * Busca el primero del nivel más alto con procesos listos.
     *
     * @return posición del proceso elegido.
     */
    private int MLFQ() {
        int elegido = 0;
        for (int i = 1; i < colaListos.size(); i++) {
            if (colaListos.get(i).getNivel() < colaListos.get(elegido).getNivel()) {
                elegido = i;
            }
        }
        return elegido;
    }

    /**
     * Baja al proceso un nivel al agotar su quantum en MLFQ; en el último nivel se mantiene.
     *
     * @param proceso proceso que agotó su quantum.
     */
    public void bajarNivel(Proceso proceso) {
        if ("MLFQ".equals(algoritmo)) {
            if (proceso.getNivel() < quantumsNivel.length - 1) {
                proceso.setNivel(proceso.getNivel() + 1);
            }
            proceso.setQuantumUsado(0);
        }
    }

    /**
     * Sube un nivel en MLFQ a los listos que alcanzaron el límite de espera; quedan al final de la cola.
     *
     * @return true si algún proceso subió de nivel.
     */
    public boolean subirNivel() {
        boolean subio = false;
        if ("MLFQ".equals(algoritmo)) {
            int revisados = 0;
            int total = colaListos.size();
            int i = 0;
            while (revisados < total) {
                Proceso proceso = colaListos.get(i);
                if (proceso.getNivel() > 0 && proceso.getTiempoEspera() >= limiteEspera) {
                    proceso.setNivel(proceso.getNivel() - 1);
                    proceso.setTiempoEspera(0);
                    colaListos.remove(i);
                    colaListos.add(proceso);
                    subio = true;
                } else {
                    i++;
                }
                revisados++;
            }
        }
        return subio;
    }

    /**
     * Suma un segundo de espera a cada proceso listo en MLFQ.
     */
    public void contarEspera() {
        if ("MLFQ".equals(algoritmo)) {
            for (Proceso listo : colaListos) {
                listo.setTiempoEspera(listo.getTiempoEspera() + 1);
            }
        }
    }

    /**
     * Busca el menor tiempo restante; empata por menor llegada y luego por menor ID.
     *
     * @return posición del proceso elegido.
     */
    private int SRT() {
        int elegido = 0;
        for (int i = 1; i < colaListos.size(); i++) {
            BCP candidato = colaListos.get(i).getBCP();
            BCP actual = colaListos.get(elegido).getBCP();
            if (candidato.getRestante() < actual.getRestante()) {
                elegido = i;
            } else if (candidato.getRestante() == actual.getRestante()) {
                if (candidato.getLlegada() < actual.getLlegada()) {
                    elegido = i;
                } else if (candidato.getLlegada() == actual.getLlegada() && candidato.getIdProceso() < actual.getIdProceso()) {
                    elegido = i;
                }
            }
        }
        return elegido;
    }

    /**
     * Sortea un boleto entre los listos; gana el proceso dueño de ese boleto.
     *
     * @return posición del proceso elegido.
     */
    private int Lottery() {
        int totalBoletos = 0;
        for (Proceso listo : colaListos) {
            totalBoletos = totalBoletos + listo.getBCP().getBoletos();
        }
        int boleto = sorteo.nextInt(totalBoletos) + 1;
        int acumulado = 0;
        int elegido = 0;
        for (int i = 0; i < colaListos.size(); i++) {
            acumulado = acumulado + colaListos.get(i).getBCP().getBoletos();
            if (boleto <= acumulado) {
                elegido = i;
                break;
            }
        }
        ultimoSorteo = "boleto " + boleto + " de " + totalBoletos + " → P" + colaListos.get(elegido).getBCP().getIdProceso();
        return elegido;
    }

    /**
     * Obtiene el resultado del último sorteo de Lottery.
     *
     * @return boleto, totalBoletos y ganador, o null si no hubo sorteo.
     */
    public String getUltimoSorteo() {
        return ultimoSorteo;
    }

    /**
     * Indica si el proceso actual debe dejar la CPU.
     * SRT expropia: cuando algún listo tiene un restante estrictamente menor que el del actual.
     * GuaranteedScheduling expropia: cuando algún listo tiene una razón de uso estrictamente menor que la del actual.
     * MLFQ expropia: cuando algún listo está en un nivel más alto que el del actual.
     * El empate no expropia.
     *
     * @param actual proceso que tiene la CPU.
     * @param instante segundo actual del reloj simulado.
     * @return true si debe expropiarse.
     */
    public boolean debeExpropiar(Proceso actual, int instante) {
        boolean expropiar = false;
        if ("SRT".equals(algoritmo) && actual != null) {
            int restanteActual = actual.getBCP().getRestante();
            for (Proceso listo : colaListos) {
                if (listo.getBCP().getRestante() < restanteActual) {
                    expropiar = true;
                }
            }
        }
        if ("GuaranteedScheduling".equals(algoritmo) && actual != null) {
            int participantes = colaListos.size() + 1;
            double razonActual = calcularRazonGS(actual.getBCP(), instante, participantes);
            for (Proceso listo : colaListos) {
                if (calcularRazonGS(listo.getBCP(), instante, participantes) < razonActual) {
                    expropiar = true;
                }
            }
        }
        if ("MLFQ".equals(algoritmo) && actual != null) {
            for (Proceso listo : colaListos) {
                if (listo.getNivel() < actual.getNivel()) {
                    expropiar = true;
                }
            }
        }
        return expropiar;
    }

    /**
     * Indica si el proceso actual debe dejar la CPU por quantum.
     * RR y Lottery expropian cuando su turno alcanza el quantum configurado.
     * MLFQ expropia cuando su turno alcanza el quantum de su nivel.
     *
     * @param actual proceso que tiene la CPU.
     * @param quantumUsado segundos ejecutados en su turno.
     * @return true si debe expropiarse.
     */
    public boolean agotoQuantum(Proceso actual, int quantumUsado) {
        boolean agotado = ("RR".equals(algoritmo) || "Lottery".equals(algoritmo)) && quantumUsado >= quantum;
        if ("MLFQ".equals(algoritmo)) {
            agotado = quantumUsado >= quantumsNivel[actual.getNivel()];
        }
        return agotado;
    }

    /**
     * Comprueba si hay procesos listos.
     *
     * @return true si la cola no está vacía.
     */
    public boolean hayProcesosListos() {
        return !colaListos.isEmpty();
    }

    /**
     * Obtiene la cola de procesos listos para CPU.
     *
     * @return cola de procesos listos.
     */
    public List<Proceso> getListos() {
        return colaListos;
    }

    /**
     * Vacía la cola de listos.
     */
    public void limpiar() {
        colaListos.clear();
    }
}
