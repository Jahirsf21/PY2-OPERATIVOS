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
    private int tasaA;
    private int tasaB;

    /**
     * Construye una cola vacía de procesos listos para CPU.
     *
     * @param algoritmo nombre del algoritmo de planificación.
     * @param quantum segundos por turno en RR.
     * @param quantumsNivel quantum de cada nivel de MLFQ, de 2 a 5 niveles.
     * @param limiteEspera segundos de espera para subir de nivel en MLFQ.
     * @param tasaA aumento de prioridad por segundo de los nuevos en SRR.
     * @param tasaB aumento de prioridad por segundo de los aceptados en SRR.
     */
    public Planificador(String algoritmo, int quantum, int[] quantumsNivel, int limiteEspera, int tasaA, int tasaB) {
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
        if (tasaA < 1 || tasaB < 0) {
            throw new IllegalArgumentException("La tasa A debe ser un entero mayor o igual a 1 y la tasa B mayor o igual a 0.");
        }
        if (tasaB >= tasaA) {
            throw new IllegalArgumentException("La tasa B debe ser menor que la tasa A.");
        }
        colaListos = new ArrayList<>();
        this.algoritmo = algoritmo;
        this.quantum = quantum;
        this.sorteo = new Random();
        this.ultimoSorteo = null;
        this.quantumsNivel = quantumsNivel;
        this.limiteEspera = limiteEspera;
        this.tasaA = tasaA;
        this.tasaB = tasaB;
    }

    /**
     * Comprueba si un algoritmo está disponible.
     *
     * @param algoritmo nombre del algoritmo.
     * @return true si puede seleccionarse.
     */
    private boolean esAlgoritmoDisponible(String algoritmo) {
        boolean disponible = "FCFS".equals(algoritmo) || "SPN".equals(algoritmo) || "Prioridad".equals(algoritmo) || "HRRN".equals(algoritmo) || "SRT".equals(algoritmo) || "RR".equals(algoritmo) || "Lottery".equals(algoritmo) || "GuaranteedScheduling".equals(algoritmo) || "MLFQ".equals(algoritmo) || "SRR".equals(algoritmo);
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
     * Obtiene el aumento de prioridad por segundo de los nuevos en SRR.
     *
     * @return tasa A.
     */
    public int getTasaA() {
        return tasaA;
    }

    /**
     * Obtiene el aumento de prioridad por segundo de los aceptados en SRR.
     *
     * @return tasa B.
     */
    public int getTasaB() {
        return tasaB;
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
            case "SRR":
                return SRR();
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
    public double calcularRazonHRRN(BCP proceso, int instante) {
        int transcurrido = instante - proceso.getLlegada();
        double razon = (double) (transcurrido + proceso.getRafaga()) / proceso.getRafaga();
        return razon;
    }

    /**
     * Compara dos procesos según el algoritmo: mayor razón de respuesta en HRRN, menor razón de uso en
     * GuaranteedScheduling y mayor prioridad en SRR; empata por menor llegada y luego por menor ID.
     *
     * @param a proceso candidato.
     * @param b proceso elegido hasta el momento.
     * @param instante segundo actual del reloj simulado.
     * @param participantes procesos que comparten la CPU, solo para GuaranteedScheduling.
     * @return true si a debe ir antes que b.
     */
    private boolean tienePrioridad(Proceso a, Proceso b, int instante, int participantes) {
        BCP bcpA = a.getBCP();
        BCP bcpB = b.getBCP();
        double valorA = 0;
        double valorB = 0;
        boolean mayorPrimero = true;
        switch (algoritmo) {
            case "HRRN":
                valorA = calcularRazonHRRN(bcpA, instante);
                valorB = calcularRazonHRRN(bcpB, instante);
                break;
            case "GuaranteedScheduling":
                valorA = calcularRazonGS(bcpA, instante, participantes);
                valorB = calcularRazonGS(bcpB, instante, participantes);
                mayorPrimero = false;
                break;
            case "SRR":
                valorA = a.getPrioridad();
                valorB = b.getPrioridad();
                break;
        }

        if (valorA > valorB) {
            return mayorPrimero;
        }
        if (valorA < valorB) {
            return !mayorPrimero;
        }
        if (bcpA.getLlegada() < bcpB.getLlegada()) {
            return true;
        }
        if (bcpA.getLlegada() > bcpB.getLlegada()) {
            return false;
        }

        if (bcpA.getIdProceso() < bcpB.getIdProceso()) {
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
            if (tienePrioridad(colaListos.get(i), colaListos.get(elegido), instante, 0)) {
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
     * Busca la menor razón de uso; empata por menor llegada y luego por menor ID.
     *
     * @param instante segundo actual del reloj simulado.
     * @return posición del proceso elegido.
     */
    private int GuaranteedScheduling(int instante) {
        int participantes = colaListos.size();
        int elegido = 0;
        for (int i = 1; i < colaListos.size(); i++) {
            if (tienePrioridad(colaListos.get(i), colaListos.get(elegido), instante, participantes)) {
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
     * Busca el primer proceso aceptado de la cola.
     *
     * @return posición del proceso elegido.
     */
    private int SRR() {
        for (int i = 0; i < colaListos.size(); i++) {
            if (colaListos.get(i).estaAceptado()) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Aumenta la prioridad de SRR: los nuevos según la tasa A y los aceptados, incluido el actual, según la tasa B.
     *
     * @param actual proceso que tiene la CPU, o null.
     */
    public void aumentarPrioridades(Proceso actual) {
        if ("SRR".equals(algoritmo)) {
            for (Proceso listo : colaListos) {
                if (listo.estaAceptado()) {
                    listo.setPrioridad(listo.getPrioridad() + tasaB);
                } else {
                    listo.setPrioridad(listo.getPrioridad() + tasaA);
                }
            }
            if (actual != null) {
                actual.setPrioridad(actual.getPrioridad() + tasaB);
            }
        }
    }

    /**
     * Acepta en SRR a los nuevos cuya prioridad alcanza la menor de los aceptados; si no hay aceptados,
     * acepta al nuevo de mayor prioridad. Los aceptados pasan al final de la cola.
     *
     * @param actual proceso que tiene la CPU, o null.
     * @return true si algún proceso fue aceptado.
     */
    public boolean aceptarNuevos(Proceso actual) {
        boolean acepto = false;
        if ("SRR".equals(algoritmo)) {
            boolean hayAceptados = false;
            int menorPrioridad = 0;
            if (actual != null) {
                hayAceptados = true;
                menorPrioridad = actual.getPrioridad();
            }
            for (Proceso listo : colaListos) {
                if (listo.estaAceptado() && (!hayAceptados || listo.getPrioridad() < menorPrioridad)) {
                    hayAceptados = true;
                    menorPrioridad = listo.getPrioridad();
                }
            }
            boolean buscarMas = true;
            while (buscarMas) {
                int elegido = -1;
                for (int i = 0; i < colaListos.size(); i++) {
                    Proceso candidato = colaListos.get(i);
                    if (!candidato.estaAceptado() && (!hayAceptados || candidato.getPrioridad() >= menorPrioridad)) {
                        if (elegido < 0 || tienePrioridad(candidato, colaListos.get(elegido), 0, 0)) {
                            elegido = i;
                        }
                    }
                }
                if (elegido < 0) {
                    buscarMas = false;
                } else {
                    Proceso nuevo = colaListos.remove(elegido);
                    nuevo.setAceptado(true);
                    colaListos.add(nuevo);
                    acepto = true;
                    if (!hayAceptados) {
                        buscarMas = false;
                    }
                }
            }
        }
        return acepto;
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
        if ("SRT".equals(algoritmo) && actual != null) {
            int restanteActual = actual.getBCP().getRestante();
            for (Proceso listo : colaListos) {
                if (listo.getBCP().getRestante() < restanteActual) {
                    return true;
                }
            }
        }
        if ("GuaranteedScheduling".equals(algoritmo) && actual != null) {
            int participantes = colaListos.size() + 1;
            double razonActual = calcularRazonGS(actual.getBCP(), instante, participantes);
            for (Proceso listo : colaListos) {
                if (calcularRazonGS(listo.getBCP(), instante, participantes) < razonActual) {
                    return true;
                }
            }
        }
        if ("MLFQ".equals(algoritmo) && actual != null) {
            for (Proceso listo : colaListos) {
                if (listo.getNivel() < actual.getNivel()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Indica si el proceso actual debe dejar la CPU por quantum.
     * RR, Lottery y SRR expropian cuando su turno alcanza el quantum configurado.
     * MLFQ expropia cuando su turno alcanza el quantum de su nivel.
     *
     * @param actual proceso que tiene la CPU.
     * @param quantumUsado segundos ejecutados en su turno.
     * @return true si debe expropiarse.
     */
    public boolean agotoQuantum(Proceso actual, int quantumUsado) {
        boolean agotado = ("RR".equals(algoritmo) || "Lottery".equals(algoritmo) || "SRR".equals(algoritmo)) && quantumUsado >= quantum;
        if ("MLFQ".equals(algoritmo)) {
            agotado = quantumUsado >= quantumsNivel[actual.getNivel()];
        }
        return agotado;
    }

    /**
     * Comprueba si hay listos esperando turno; en SRR solo cuentan los aceptados.
     *
     * @return true si alguno espera su turno.
     */
    public boolean hayAceptadosEsperando() {
        for (Proceso listo : colaListos) {
            if (!"SRR".equals(algoritmo) || listo.estaAceptado()) {
                return true;
            }
        }
        return false;
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
