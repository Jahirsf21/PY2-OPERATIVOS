package com.mycompany.t1operativos.modelo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Coordina los procesos de la Mini PC.
 *
 * @author Deislher Sánchez Funez
 * @author Natalia Granados Rosales
 */
public class GestorProcesos {
    private Memoria memoria;
    private AsignadorMemoria asignador;
    private Disco disco;
    private CargadorArchivos cargador;
    private GestorArchivos gestorArchivos;
    private List<Proceso> procesosRegistrados;
    private Queue<Proceso> colaTrabajos;
    private int reloj;
    private boolean corridaIniciada;
    private List<ResultadoCorrida> historial;
    private List<Nucleo> nucleos;
    private List<String> erroresPaso;

    /**
     * Construye los recursos de la simulación.
     *
     * @param tamañoMemoria posiciones de RAM.
     * @param tamañoDisco posiciones de disco.
     * @param tamañoMemoriaVirtual posiciones reservadas en disco.
     * @param algoritmo algoritmo de planificación.
     * @param quantum segundos por turno en RR.
     * @param quantumsNivel quantum de cada nivel de MLFQ.
     * @param limiteEspera segundos de espera para subir de nivel en MLFQ.
     * @param tasaA aumento de prioridad por segundo de los nuevos en SRR.
     * @param tasaB aumento de prioridad por segundo de los aceptados en SRR.
     * @param cantidadCPU cantidad de CPU, de 1 a 5.
     * @param tipoAsignacion tipo de asignación de la zona de usuario.
     * @param cantidadParticiones cantidad de particiones para Fija iguales.
     * @param tamañosParticion tamaño de cada partición para Fija desiguales.
     */
    public GestorProcesos(int tamañoMemoria, int tamañoDisco, int tamañoMemoriaVirtual, String algoritmo, int quantum, int[] quantumsNivel, int limiteEspera, int tasaA, int tasaB, int cantidadCPU, String tipoAsignacion, int cantidadParticiones, int[] tamañosParticion) {
        reiniciarRecursos(tamañoMemoria, tamañoDisco, tamañoMemoriaVirtual, algoritmo, quantum, quantumsNivel, limiteEspera, tasaA, tasaB, cantidadCPU, tipoAsignacion, cantidadParticiones, tamañosParticion);
        historial = new ArrayList<>();
    }

    /**
     * Carga los archivos y conserva los aceptados.
     *
     * @param archivos selección en el orden recibido.
     * @return mensajes de archivos rechazados.
     */
    public List<String> cargarProgramas(List<File> archivos) {
        if (archivos == null || archivos.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar archivos para cargar.");
        }
        List<String> mensajes = new ArrayList<>();
        for (File archivo : archivos) {
            try {
                cargarPrograma(archivo);
            } catch (IOException | IllegalArgumentException | IllegalStateException ex) {
                String nombre = "Archivo nulo";
                if (archivo != null) {
                    nombre = archivo.getName();
                }
                mensajes.add(nombre + ": " + ex.getMessage());
            }
        }
        return mensajes;
    }

    /**
     * Simula el segundo [reloj, reloj + 1): admite llegadas, despacha y
     * ejecuta una instrucción, o registra ocio si no hay procesos listos.
     *
     * @return salida de INT 10H, o null si no hubo salida.
     */
    public String ejecutarPaso() {
        admitirPaso();
        despacharPaso();
        String salida = completarPaso();
        if (!erroresPaso.isEmpty()) {
            String mensaje = erroresPaso.get(0);
            for (int i = 1; i < erroresPaso.size(); i++) {
                mensaje = mensaje + "\n" + erroresPaso.get(i);
            }
            throw new IllegalStateException(mensaje);
        }
        return salida;
    }

    /**
     * Admite las llegadas en RAM y expropia al actual si corresponde.
     *
     * @return true si algún proceso cambió de estado.
     */
    public boolean admitirPaso() {
        if (!puedeEjecutar()) {
            throw new IllegalStateException("No hay procesos ejecutables.");
        }
        corridaIniciada = true;
        boolean cambio = false;
        boolean[] agotados = new boolean[nucleos.size()];
        for (int i = 0; i < nucleos.size(); i++) {
            Nucleo nucleo = nucleos.get(i);
            Planificador planificador = nucleo.getPlanificador();
            Proceso actual = nucleo.getDespachador().getActual();
            boolean agotado = actual != null && planificador.agotoQuantum(actual, nucleo.getQuantumUsado());
            if (agotado) {
                planificador.bajarNivel(actual);
            }
            if (agotado && planificador.hayAceptadosEsperando()) {
                expropiarActual(nucleo);
                cambio = true;
                agotado = false;
            }
            if (planificador.subirNivel()) {
                cambio = true;
            }
            agotados[i] = agotado;
        }
        admitirLlegadas();
        if (admitirEnMemoria()) {
            cambio = true;
        }
        for (int i = 0; i < nucleos.size(); i++) {
            Nucleo nucleo = nucleos.get(i);
            Planificador planificador = nucleo.getPlanificador();
            if (planificador.aceptarNuevos(nucleo.getDespachador().getActual())) {
                cambio = true;
            }
            if (agotados[i]) {
                expropiarActual(nucleo);
                cambio = true;
            } else if (planificador.debeExpropiar(nucleo.getDespachador().getActual(), reloj)) {
                interrumpirActual(nucleo);
                cambio = true;
            }
        }
        return cambio;
    }

    /**
     * Entrega cada CPU libre al siguiente proceso listo de su cola.
     *
     * @return true si despachó algún proceso.
     */
    public boolean despacharPaso() {
        boolean despacho = false;
        for (Nucleo nucleo : nucleos) {
            Planificador planificador = nucleo.getPlanificador();
            if (nucleo.getDespachador().getActual() == null && planificador.hayProcesosListos()) {
                Proceso siguiente = planificador.obtenerSiguiente(reloj);
                nucleo.getDespachador().despachar(siguiente, reloj);
                nucleo.setQuantumUsado(siguiente.getQuantumUsado());
                siguiente.setQuantumUsado(0);
                siguiente.setTiempoEspera(0);
                despacho = true;
            }
        }
        return despacho;
    }

    /**
     * Ejecuta una instrucción en cada CPU, o registra su ocio, y avanza el reloj un segundo.
     *
     * @return salidas de INT 10H, o null si no hubo salida.
     */
    public String completarPaso() {
        if (!puedeEjecutar()) {
            throw new IllegalStateException("No hay procesos ejecutables.");
        }
        for (Nucleo nucleo : nucleos) {
            nucleo.getPlanificador().contarEspera();
            nucleo.getPlanificador().aumentarPrioridades(nucleo.getDespachador().getActual());
        }
        reloj++;
        erroresPaso = new ArrayList<>();
        String salida = null;
        for (Nucleo nucleo : nucleos) {
            String salidaNucleo = null;
            try {
                salidaNucleo = ejecutarNucleo(nucleo);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                String error = ex.getMessage();
                if (nucleos.size() > 1) {
                    error = "CPU " + nucleo.getNumero() + ": " + error;
                }
                erroresPaso.add(error);
            }
            if (salidaNucleo != null && nucleos.size() > 1) {
                salidaNucleo = "CPU " + nucleo.getNumero() + ": " + salidaNucleo;
            }
            if (salidaNucleo != null && salida == null) {
                salida = salidaNucleo;
            } else if (salidaNucleo != null) {
                salida = salida + "\n" + salidaNucleo;
            }
        }
        guardarResultado();
        return salida;
    }

    /**
     * Ejecuta una instrucción del proceso actual de una CPU, o le suma un segundo de ocio.
     *
     * @param nucleo CPU que ejecuta.
     * @return salida de INT 10H, o null si no hubo salida.
     */
    private String ejecutarNucleo(Nucleo nucleo) {
        Proceso proceso = nucleo.getDespachador().getActual();
        if (proceso == null) {
            nucleo.aumentarOcio();
            return null;
        }
        BCP bcp = proceso.getBCP();
        CPU cpu = nucleo.getDespachador().getCPU();
        try {
            if (cpu.getPc() < bcp.getInicioMemoria() || cpu.getPc() > bcp.getFinMemoria()) {
                throw new IllegalStateException("El PC está fuera de los límites del programa.");
            }
            String[] instruccion = memoria.leer(cpu.getPc());
            if (instruccion == null) {
                throw new IllegalStateException("No existe una instrucción válida en la posición " + cpu.getPc() + ".");
            }
            cpu.cargarInstruccion(instruccion);
            bcp.aumentarTiempoEmpleado();
            nucleo.aumentarQuantumUsado();
            String salida = ejecutarInstruccion(nucleo, instruccion);
            bcp.guardarContexto(cpu);
            if ("TERMINADO".equals(bcp.getEstado()) || !tieneInstrucciones(bcp)) {
                finalizarProceso(proceso);
            } else {
                escribirBCPEnMemoria(proceso);
            }
            return salida;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            if (proceso == nucleo.getDespachador().getActual()) {
                finalizarProceso(proceso);
            }
            throw ex;
        }
    }

    /**
     * Inicia otra corrida desde cero con los mismos programas, sus llegadas, prioridades y boletos.
     *
     * @param tamañoMemoria posiciones de RAM.
     * @param tamañoDisco posiciones de disco.
     * @param tamañoMemoriaVirtual posiciones reservadas en disco.
     * @param algoritmo algoritmo de planificación.
     * @param quantum segundos por turno en RR.
     * @param quantumsNivel quantum de cada nivel de MLFQ.
     * @param limiteEspera segundos de espera para subir de nivel en MLFQ.
     * @param tasaA aumento de prioridad por segundo de los nuevos en SRR.
     * @param tasaB aumento de prioridad por segundo de los aceptados en SRR.
     * @param cantidadCPU cantidad de CPU, de 1 a 5.
     * @param tipoAsignacion tipo de asignación de la zona de usuario.
     * @param cantidadParticiones cantidad de particiones para Fija iguales.
     * @param tamañosParticion tamaño de cada partición para Fija desiguales.
     * @return mensajes de programas que no se pudieron registrar.
     */
    public List<String> nuevaCorrida(int tamañoMemoria, int tamañoDisco, int tamañoMemoriaVirtual, String algoritmo, int quantum, int[] quantumsNivel, int limiteEspera, int tasaA, int tasaB, int cantidadCPU, String tipoAsignacion, int cantidadParticiones, int[] tamañosParticion) {
        if (!estaCorridaTerminada()) {
            throw new IllegalStateException("Solo se inicia una nueva corrida cuando la actual terminó.");
        }
        Disco discoAnterior = disco;
        List<Proceso> procesosAnteriores = procesosRegistrados;
        reiniciarRecursos(tamañoMemoria, tamañoDisco, tamañoMemoriaVirtual, algoritmo, quantum, quantumsNivel, limiteEspera, tasaA, tasaB, cantidadCPU, tipoAsignacion, cantidadParticiones, tamañosParticion);
        List<String> mensajes = new ArrayList<>();
        for (Proceso anterior : procesosAnteriores) {
            String[] entrada = discoAnterior.leer(anterior.getPosicionIndice());
            int inicio = Integer.parseInt(entrada[1]);
            int tamaño = Integer.parseInt(entrada[2]);
            try {
                if (!asignador.tieneEspacioPara(tamaño)) {
                    throw new IllegalArgumentException("El programa de " + tamaño + " posiciones no cabe con el tipo de asignación " + asignador.getTipoAsignacion() + ".");
                }
                Nucleo nucleo = null;
                int cpuAnterior = anterior.getBCP().getCpuActual();
                if (cpuAnterior >= 1 && cpuAnterior <= nucleos.size() && contarSinTerminar(nucleos.get(cpuAnterior - 1)) < 5) {
                    nucleo = nucleos.get(cpuAnterior - 1);
                } else {
                    nucleo = elegirNucleo();
                }
                if (nucleo == null) {
                    throw new IllegalStateException("Todas las CPU tienen cinco procesos no terminados.");
                }
                List<String[]> instrucciones = new ArrayList<>();
                for (int posicion = inicio; posicion < inicio + tamaño; posicion++) {
                    instrucciones.add(discoAnterior.leer(posicion));
                }
                BCP bcpAnterior = anterior.getBCP();
                int posicionIndice = gestorArchivos.registrarPrograma(entrada[0], instrucciones);
                BCP bcp = new BCP(bcpAnterior.getIdProceso(), bcpAnterior.getPrioridad(), tamaño, bcpAnterior.getLlegada());
                bcp.setBoletos(bcpAnterior.getBoletos());
                bcp.setCpuActual(nucleo.getNumero());
                procesosRegistrados.add(new Proceso(bcp, posicionIndice));
            } catch (IllegalArgumentException | IllegalStateException ex) {
                mensajes.add(entrada[0] + ": " + ex.getMessage());
            }
        }
        return mensajes;
    }

    /**
     * Reinicia la simulación conservando capacidades.
     */
    public void limpiar() {
        Planificador planificador = nucleos.get(0).getPlanificador();
        reiniciarRecursos(memoria.getTamañoTotal(), disco.getTamañoTotal(), disco.getTamañoTotal() - disco.getInicioMemoriaVirtual(), planificador.getAlgoritmo(), planificador.getQuantum(), planificador.getQuantumsNivel(), planificador.getLimiteEspera(), planificador.getTasaA(), planificador.getTasaB(), nucleos.size(), asignador.getTipoAsignacion(), asignador.getDireccionesInicio().length, asignador.getTamañosParticion());
        historial = new ArrayList<>();
    }

    /**
     * Obtiene la RAM compartida.
     *
     * @return memoria de la simulación.
     */
    public Memoria getMemoria() {
        return memoria;
    }

    /**
     * Obtiene el asignador de la zona de usuario.
     *
     * @return asignador de la simulación.
     */
    public AsignadorMemoria getAsignador() {
        return asignador;
    }

    /**
     * Obtiene el disco activo.
     *
     * @return disco de la simulación.
     */
    public Disco getDisco() {
        return disco;
    }

    /**
     * Obtiene una CPU del simulador.
     *
     * @param numero número de la CPU, desde 1.
     * @return CPU del despachador de ese núcleo.
     */
    public CPU getCPU(int numero) {
        return nucleos.get(numero - 1).getDespachador().getCPU();
    }

    /**
     * Obtiene el proceso que tiene una CPU.
     *
     * @param numero número de la CPU, desde 1.
     * @return proceso actual de esa CPU, o null.
     */
    public Proceso getActual(int numero) {
        return nucleos.get(numero - 1).getDespachador().getActual();
    }

    /**
     * Obtiene los errores de las CPU en el último segundo ejecutado.
     *
     * @return mensajes de error, vacío si no hubo.
     */
    public List<String> getErroresPaso() {
        return erroresPaso;
    }

    /**
     * Obtiene las CPU del simulador.
     *
     * @return núcleos en orden de número.
     */
    public List<Nucleo> getNucleos() {
        return nucleos;
    }

    /**
     * Obtiene todos los procesos registrados.
     *
     * @return lista en orden de carga, incluidos los terminados.
     */
    public List<Proceso> getProcesosRegistrados() {
        return procesosRegistrados;
    }

    /**
     * Comprueba si se puede avanzar el reloj.
     *
     * @return true si queda algún proceso sin terminar, aunque aún no haya llegado.
     */
    public boolean puedeEjecutar() {
        return hayProcesosSinTerminar();
    }

    /**
     * Obtiene el algoritmo de planificación.
     *
     * @return nombre del algoritmo.
     */
    public String getAlgoritmo() {
        return nucleos.get(0).getPlanificador().getAlgoritmo();
    }

    /**
     * Obtiene el quantum de planificación.
     *
     * @return segundos por turno.
     */
    public int getQuantum() {
        return nucleos.get(0).getPlanificador().getQuantum();
    }

    /**
     * Obtiene el quantum de cada nivel de MLFQ.
     *
     * @return quantums por nivel.
     */
    public int[] getQuantumsNivel() {
        return nucleos.get(0).getPlanificador().getQuantumsNivel();
    }

    /**
     * Obtiene los segundos de espera para subir de nivel en MLFQ.
     *
     * @return límite de espera.
     */
    public int getLimiteEspera() {
        return nucleos.get(0).getPlanificador().getLimiteEspera();
    }

    /**
     * Obtiene el aumento de prioridad por segundo de los nuevos en SRR.
     *
     * @return tasa A.
     */
    public int getTasaA() {
        return nucleos.get(0).getPlanificador().getTasaA();
    }

    /**
     * Obtiene el aumento de prioridad por segundo de los aceptados en SRR.
     *
     * @return tasa B.
     */
    public int getTasaB() {
        return nucleos.get(0).getPlanificador().getTasaB();
    }

    /**
     * Obtiene los segundos del quantum de su nivel que lleva usados un proceso.
     *
     * @param proceso proceso cargado en RAM.
     * @return segundos usados; del turno en curso si tiene la CPU.
     */
    public int getQuantumUsado(Proceso proceso) {
        Nucleo nucleo = getNucleo(proceso);
        if (proceso == nucleo.getDespachador().getActual()) {
            return nucleo.getQuantumUsado();
        }
        return proceso.getQuantumUsado();
    }

    /**
     * Obtiene el resultado del último sorteo de Lottery en una CPU.
     *
     * @param numero número de la CPU, desde 1.
     * @return boleto, total y ganador, o null si no hubo sorteo.
     */
    public String getUltimoSorteo(int numero) {
        return nucleos.get(numero - 1).getPlanificador().getUltimoSorteo();
    }

    /**
     * Obtiene la razón de uso de un proceso en el reloj actual.
     *
     * @param proceso proceso cargado en RAM.
     * @return servicio / ((reloj − llegada) / n), o 0 si acaba de llegar.
     */
    public double getRazon(Proceso proceso) {
        Nucleo nucleo = getNucleo(proceso);
        int participantes = nucleo.getPlanificador().getListos().size();
        if (nucleo.getDespachador().getActual() != null) {
            participantes++;
        }
        return nucleo.getPlanificador().calcularRazonGS(proceso.getBCP(), reloj, participantes);
    }

    /**
     * Obtiene el reloj simulado.
     *
     * @return segundos transcurridos desde cero.
     */
    public int getReloj() {
        return reloj;
    }

    /**
     * Obtiene el ocio de todas las CPU.
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
     * Obtiene la CPU a la que está asignado un proceso.
     *
     * @param proceso proceso registrado.
     * @return núcleo de su CPU actual.
     */
    private Nucleo getNucleo(Proceso proceso) {
        return nucleos.get(proceso.getBCP().getCpuActual() - 1);
    }

    /**
     * Cuenta los procesos sin terminar asignados a una CPU.
     *
     * @param nucleo CPU consultada.
     * @return cantidad de procesos no terminados.
     */
    private int contarSinTerminar(Nucleo nucleo) {
        int cantidad = 0;
        for (Proceso proceso : procesosRegistrados) {
            BCP bcp = proceso.getBCP();
            if (bcp.getCpuActual() == nucleo.getNumero() && !"TERMINADO".equals(bcp.getEstado())) {
                cantidad++;
            }
        }
        return cantidad;
    }

    /**
     * Elige la CPU con menos procesos sin terminar; empata por más ocio y luego por menor número.
     *
     * @return núcleo elegido, o null si todas tienen cinco procesos sin terminar.
     */
    private Nucleo elegirNucleo() {
        Nucleo elegido = null;
        int menorCantidad = 0;
        for (Nucleo nucleo : nucleos) {
            int cantidad = contarSinTerminar(nucleo);
            if (cantidad < 5 && (elegido == null || cantidad < menorCantidad || (cantidad == menorCantidad && nucleo.getSegundosOcio() > elegido.getSegundosOcio()))) {
                elegido = nucleo;
                menorCantidad = cantidad;
            }
        }
        return elegido;
    }

    /**
     * Obtiene los resultados de las corridas terminadas.
     *
     * @return historial de corridas.
     */
    public List<ResultadoCorrida> getHistorial() {
        return historial;
    }

    /**
     * Comprueba si la corrida empezó y todos sus procesos terminaron.
     *
     * @return true si la corrida terminó.
     */
    public boolean estaCorridaTerminada() {
        boolean terminada = corridaIniciada && !hayProcesosSinTerminar();
        return terminada;
    }

    /**
     * Obtiene el resultado de la corrida en curso o, si terminó, el guardado en el historial.
     *
     * @return resultado de la corrida actual.
     */
    public ResultadoCorrida getCorridaActual() {
        if (estaCorridaTerminada()) {
            return historial.get(historial.size() - 1);
        }
        return new ResultadoCorrida(historial.size() + 1, nucleos, asignador, procesosRegistrados, reloj);
    }

    /**
     * Guarda el resultado de la corrida en el historial si terminó.
     */
    private void guardarResultado() {
        if (estaCorridaTerminada()) {
            historial.add(new ResultadoCorrida(historial.size() + 1, nucleos, asignador, procesosRegistrados, reloj));
        }
    }

    /**
     * Comprueba si la llegada y la prioridad aún se pueden editar.
     *
     * @return true antes del primer paso.
     */
    public boolean puedeEditarTrabajos() {
        return !corridaIniciada;
    }

    /**
     * Cambia la llegada de un proceso antes del primer paso.
     *
     * @param idProceso identificador del proceso.
     * @param llegada segundo de llegada, no negativo.
     */
    public void cambiarLlegada(int idProceso, int llegada) {
        buscarEditable(idProceso).setLlegada(llegada);
    }

    /**
     * Cambia la prioridad de un proceso antes del primer paso.
     *
     * @param idProceso identificador del proceso.
     * @param prioridad entero de 1 a 10; mayor es más prioritario.
     */
    public void cambiarPrioridad(int idProceso, int prioridad) {
        buscarEditable(idProceso).setPrioridad(prioridad);
    }

    /**
     * Cambia los boletos de un proceso antes del primer paso.
     *
     * @param idProceso identificador del proceso.
     * @param boletos entero de 1 a 100.
     */
    public void cambiarBoletos(int idProceso, int boletos) {
        buscarEditable(idProceso).setBoletos(boletos);
    }

    /**
     * Busca el BCP de un proceso cuyos datos aún pueden editarse.
     *
     * @param idProceso identificador del proceso.
     * @return BCP del proceso.
     */
    private BCP buscarEditable(int idProceso) {
        if (corridaIniciada) {
            throw new IllegalStateException("La llegada, la prioridad y los boletos solo se editan antes del primer paso.");
        }
        for (Proceso proceso : procesosRegistrados) {
            if (proceso.getBCP().getIdProceso() == idProceso) {
                return proceso.getBCP();
            }
        }
        throw new IllegalArgumentException("No existe el proceso " + idProceso + ".");
    }

    /**
     * Comprueba si quedan procesos sin terminar.
     *
     * @return true si algún proceso no terminó.
     */
    public boolean hayProcesosSinTerminar() {
        for (Proceso proceso : procesosRegistrados) {
            if (!"TERMINADO".equals(proceso.getBCP().getEstado())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Carga y registra un programa.
     *
     * @param archivo archivo ensamblador.
     * @throws IOException si falla su lectura.
     */
    private void cargarPrograma(File archivo) throws IOException {
        if (archivo == null) {
            throw new IllegalArgumentException("Debe indicar un archivo.");
        }
        List<String[]> instrucciones = cargador.cargarArchivo(archivo.getAbsolutePath());
        List<String> errores = cargador.getErrores();
        if (!errores.isEmpty()) {
            String mensaje = "El archivo contiene errores:\n";
            for (int i = 0; i < errores.size(); i++) {
                if (i > 0) {
                    mensaje = mensaje + "\n";
                }
                mensaje = mensaje + errores.get(i);
            }
            throw new IllegalArgumentException(mensaje);
        }
        if (!asignador.tieneEspacioPara(instrucciones.size())) {
            throw new IllegalArgumentException("El programa de " + instrucciones.size() + " posiciones no cabe con el tipo de asignación " + asignador.getTipoAsignacion() + ".");
        }
        if (estaCorridaTerminada()) {
            throw new IllegalStateException("La corrida terminó; use Nueva corrida para ejecutar otra.");
        }
        Nucleo nucleo = elegirNucleo();
        if (nucleo == null) {
            throw new IllegalStateException("Todas las CPU tienen cinco procesos no terminados.");
        }
        int id = 1;
        if (!procesosRegistrados.isEmpty()) {
            id = procesosRegistrados.get(procesosRegistrados.size() - 1).getBCP().getIdProceso() + 1;
        }
        int posicionIndice = gestorArchivos.registrarPrograma(archivo.getName(), instrucciones);
        BCP bcp = new BCP(id, 1, instrucciones.size(), reloj);
        bcp.setCpuActual(nucleo.getNumero());
        procesosRegistrados.add(new Proceso(bcp, posicionIndice));
    }

    /**
     * Pasa a la cola de trabajos los procesos cuya llegada ya ocurrió, por
     * llegada y luego por orden de registro.
     */
    private void admitirLlegadas() {
        while (true) {
            Proceso siguiente = null;
            for (Proceso proceso : procesosRegistrados) {
                BCP bcp = proceso.getBCP();
                if ("NUEVO".equals(bcp.getEstado()) && bcp.getLlegada() <= reloj && !colaTrabajos.contains(proceso)) {
                    if (siguiente == null || bcp.getLlegada() < siguiente.getBCP().getLlegada()) {
                        siguiente = proceso;
                    }
                }  
            }
            if (siguiente == null) {
                break;
            }
            colaTrabajos.add(siguiente);
        }
    }

    /**
     * Busca un bloque libre en RAM.
     *
     * @param inicio primera posición del rango.
     * @param limite posición donde termina la búsqueda, sin incluirla.
     * @param cantidad posiciones requeridas.
     * @return inicio libre, o -1.
     */
    private int buscarBloqueMemoria(int inicio, int limite, int cantidad) {
        int libres = 0;
        for (int posicion = inicio; posicion < limite; posicion++) {
            if (memoria.leer(posicion) == null) {
                libres++;
            } else {
                libres = 0;
            }
            if (libres == cantidad) {
                int inicioBloque = posicion - cantidad + 1;
                return inicioBloque;
            }
        }
        return -1;
    }

    /**
     * Carga en RAM los trabajos de la cola que caben, desde el primero.
     *
     * @return true si algún proceso pasó a LISTO.
     */
    private boolean admitirEnMemoria() {
        boolean admitio = false;
        while (!colaTrabajos.isEmpty()) {
            Proceso proceso = colaTrabajos.peek();
            BCP bcp = proceso.getBCP();
            String[] entrada = disco.leer(proceso.getPosicionIndice());
            if (entrada == null) {
                throw new IllegalStateException("El archivo pendiente no tiene una entrada válida en disco.");
            }
            int direccion = Integer.parseInt(entrada[1]);
            int cantidadInstrucciones = Integer.parseInt(entrada[2]);
            int posicionBCP = buscarBloqueMemoria(0, memoria.getInicioUsuario(), getCamposBCP().length);
            if (posicionBCP < 0) {
                break;
            }
            int inicioPrograma = asignador.asignar(cantidadInstrucciones);
            if (inicioPrograma < 0) {
                break;
            }
            for (int i = 0; i < cantidadInstrucciones; i++) {
                memoria.escribirUsuario(inicioPrograma + i, disco.leer(direccion + i));
            }
            bcp.asignarMemoria(inicioPrograma, inicioPrograma + cantidadInstrucciones - 1);
            proceso.asignarPosicionBCP(posicionBCP);
            bcp.setEstadoListo();
            escribirBCPEnMemoria(proceso);
            colaTrabajos.remove();
            getNucleo(proceso).getPlanificador().agregarListo(proceso);
            admitio = true;
        }
        actualizarEnlacesBCP();
        return admitio;
    }

    /**
     * Obtiene los campos del BCP.
     *
     * @return etiquetas representadas en kernel.
     */
    private String[] getCamposBCP() {
        return new String[]{"idProceso", "estado", "prioridad", "boletos", "pc", "inicioMemoria", "finMemoria",
            "ir", "ac", "ax", "bx", "cx", "dx", "ah", "al", "flag", "pila1", "pila2", "pila3", "pila4",
            "pila5", "punteroPila", "cpuActual", "llegada", "rafaga", "tiempoInicio", "tiempoFinal",
            "tiempoServicio", "direccionSiguienteBCP", "archivosAbiertos"};
    }

    /**
     * Escribe el BCP en el kernel.
     *
     * @param proceso proceso cargado en RAM.
     */
    private void escribirBCPEnMemoria(Proceso proceso) {
        String[] nombres = getCamposBCP();
        int posicion = proceso.getPosicionBCP();
        BCP bcp = proceso.getBCP();
        String[] valores = {String.valueOf(bcp.getIdProceso()), bcp.getEstado(), String.valueOf(bcp.getPrioridad()),
            String.valueOf(bcp.getBoletos()),
            String.valueOf(bcp.getPc()), String.valueOf(bcp.getInicioMemoria()), String.valueOf(bcp.getFinMemoria()),
            bcp.getIrToString(), String.valueOf(bcp.getAc()), String.valueOf(bcp.getAx()), String.valueOf(bcp.getBx()),
            String.valueOf(bcp.getCx()), bcp.getDx(), String.valueOf(bcp.getAh()), String.valueOf(bcp.getAl()),
            String.valueOf(bcp.esIgual()), String.valueOf(bcp.getValorPila(0)),
            String.valueOf(bcp.getValorPila(1)), String.valueOf(bcp.getValorPila(2)), String.valueOf(bcp.getValorPila(3)),
            String.valueOf(bcp.getValorPila(4)), String.valueOf(bcp.getPunteroPila()), String.valueOf(bcp.getCpuActual()),
            String.valueOf(bcp.getLlegada()), String.valueOf(bcp.getRafaga()),
            String.valueOf(bcp.getTiempoInicio()), String.valueOf(bcp.getTiempoFinal()),
            String.valueOf(bcp.getTiempoEmpleadoSegundos()), String.valueOf(bcp.getDireccionSiguienteBCP()),
            bcp.getArchivosAbiertos().toString()};
        for (int i = 0; i < nombres.length; i++) {
            memoria.escribirKernel(posicion + i, nombres[i], valores[i]);
        }
    }

    /**
     * Actualiza los enlaces de los BCP en RAM.
     */
    private void actualizarEnlacesBCP() {
        for (Proceso proceso : procesosRegistrados) {
            if (proceso.getPosicionBCP() < 0) {
                proceso.getBCP().setDireccionSiguienteBCP(-1);
                continue;
            }
            int siguiente = -1;
            for (Proceso otro : procesosRegistrados) {
                int posicion = otro.getPosicionBCP();
                if (posicion > proceso.getPosicionBCP()) {
                    if (siguiente == -1 || posicion < siguiente) {
                        siguiente = posicion;
                    }
                }
            }
            proceso.getBCP().setDireccionSiguienteBCP(siguiente);
            escribirBCPEnMemoria(proceso);
        }
    }

    /**
     * Ejecuta una instrucción completa en un segundo.
     *
     * @param nucleo CPU que ejecuta.
     * @param instruccion instrucción cargada en IR.
     * @return salida de pantalla, o null.
     */
    private String ejecutarInstruccion(Nucleo nucleo, String[] instruccion) {
        CPU cpu = nucleo.getDespachador().getCPU();
        cpu.ejecutarInstruccion(instruccion);
        String salida = null;
        if (cpu.getInterrupcion() != null) {
            salida = atenderInterrupcion(nucleo, cpu.getInterrupcion());
        }
        return salida;
    }

    /**
     * Atiende la interrupción generada por la CPU.
     *
     * @param nucleo CPU que generó la interrupción.
     * @param codigo código de la interrupción.
     * @return salida de pantalla, o null.
     */
    private String atenderInterrupcion(Nucleo nucleo, String codigo) {
        String salida = null;
        switch (codigo) {
            case "20H":
                nucleo.getDespachador().getActual().getBCP().setEstadoTerminado(reloj);
                break;
            case "10H":
                salida = nucleo.getDespachador().getCPU().getDx();
                break;
            case "21H":
                ejecutarServicioArchivo(nucleo);
                break;
            default:
                throw new IllegalArgumentException("La interrupción INT " + codigo + " todavía no está implementada.");
        }
        return salida;
    }

    /**
     * Atiende el servicio de archivos seleccionado en AH.
     *
     * @param nucleo CPU que pidió el servicio.
     */
    private void ejecutarServicioArchivo(Nucleo nucleo) {
        BCP bcp = nucleo.getDespachador().getActual().getBCP();
        CPU cpu = nucleo.getDespachador().getCPU();
        switch (cpu.getAh()) {
            case 60:
                gestorArchivos.crearArchivo(cpu.getDx());
                break;
            case 61:
                gestorArchivos.abrirArchivo(bcp, cpu.getDx());
                break;
            case 77:
                cpu.setAl(gestorArchivos.leerArchivo(bcp, cpu.getDx()));
                break;
            case 64:
                gestorArchivos.escribirArchivo(bcp, cpu.getDx(), cpu.getAl());
                break;
            case 65:
                gestorArchivos.eliminarArchivo(bcp, cpu.getDx());
                break;
            default:
                throw new IllegalArgumentException("AH no selecciona un servicio de archivos válido: " + cpu.getAh() + ".");
        }
    }

    /**
     * Comprueba si quedan instrucciones.
     *
     * @param bcp contexto del proceso.
     * @return false solo si PC está en fin más uno.
     */
    private boolean tieneInstrucciones(BCP bcp) {
        if (bcp.getPc() < bcp.getInicioMemoria() || bcp.getPc() > bcp.getFinMemoria() + 1) {
            throw new IllegalStateException("El PC está fuera de los límites del programa.");
        }
        boolean tiene = bcp.getPc() <= bcp.getFinMemoria();
        return tiene;
    }

    /**
     * Expropia al proceso actual de una CPU: vuelve a LISTO con su contexto guardado,
     * conserva su memoria y regresa a la cola de listos.
     *
     * @param nucleo CPU del proceso.
     */
    private void expropiarActual(Nucleo nucleo) {
        Proceso expropiado = nucleo.getDespachador().expropiarActual();
        escribirBCPEnMemoria(expropiado);
        nucleo.getPlanificador().agregarListo(expropiado);
    }

    /**
     * Interrumpe al actual de una CPU porque otro listo tiene preferencia y lo devuelve a listos.
     *
     * @param nucleo CPU del proceso.
     */
    private void interrumpirActual(Nucleo nucleo) {
        Proceso interrumpido = nucleo.getDespachador().expropiarActual();
        escribirBCPEnMemoria(interrumpido);
        nucleo.getPlanificador().reinsertarInterrumpido(interrumpido, nucleo.getQuantumUsado());
    }

    /**
     * Finaliza y libera un proceso.
     *
     * @param proceso proceso que termina.
     */
    private void finalizarProceso(Proceso proceso) {
        BCP bcp = proceso.getBCP();
        Nucleo nucleo = getNucleo(proceso);
        boolean esActual = proceso == nucleo.getDespachador().getActual();
        int posicion = proceso.getPosicionBCP();
        int cantidadCampos = getCamposBCP().length;
        if (posicion < 0) {
            throw new IllegalStateException("El proceso no tiene memoria asignada para liberar.");
        }
        bcp.setEstadoTerminado(reloj);
        gestorArchivos.cerrarArchivos(bcp);
        if (esActual) {
            nucleo.getDespachador().retirarActual();
        }
        memoria.liberarUsuario(bcp.getInicioMemoria(), bcp.getAlcance());
        asignador.liberar(bcp.getInicioMemoria());
        memoria.liberarKernel(posicion, cantidadCampos);
        proceso.liberarPosicionBCP();
        bcp.setDireccionSiguienteBCP(-1);
        actualizarEnlacesBCP();
        admitirEnMemoria();
    }

    /**
     * Reinicia los recursos.
     *
     * @param tamañoMemoria posiciones de RAM.
     * @param tamañoDisco posiciones de disco.
     * @param tamañoMemoriaVirtual posiciones reservadas en disco.
     * @param algoritmo algoritmo de planificación.
     * @param quantum segundos por turno en RR.
     * @param quantumsNivel quantum de cada nivel de MLFQ.
     * @param limiteEspera segundos de espera para subir de nivel en MLFQ.
     * @param tasaA aumento de prioridad por segundo de los nuevos en SRR.
     * @param tasaB aumento de prioridad por segundo de los aceptados en SRR.
     * @param cantidadCPU cantidad de CPU, de 1 a 5.
     * @param tipoAsignacion tipo de asignación de la zona de usuario.
     * @param cantidadParticiones cantidad de particiones para Fija iguales.
     * @param tamañosParticion tamaño de cada partición para Fija desiguales.
     */
    private void reiniciarRecursos(int tamañoMemoria, int tamañoDisco, int tamañoMemoriaVirtual, String algoritmo, int quantum, int[] quantumsNivel, int limiteEspera, int tasaA, int tasaB, int cantidadCPU, String tipoAsignacion, int cantidadParticiones, int[] tamañosParticion) {
        if (tamañoMemoria < 128) {
            throw new IllegalArgumentException("La RAM debe ser al menos 128 posiciones.");
        }
        if (cantidadCPU < 1 || cantidadCPU > 5) {
            throw new IllegalArgumentException("La cantidad de CPU debe estar entre 1 y 5.");
        }
        int tamañoKernel = tamañoMemoria / 4;
        int tamañoIndices = tamañoDisco / 20;
        if (tamañoMemoriaVirtual < 0 || tamañoMemoriaVirtual > tamañoDisco - tamañoIndices) {
            throw new IllegalArgumentException("La memoria virtual debe estar entre 0 y " + (tamañoDisco - tamañoIndices) + " posiciones.");
        }
        Memoria nuevaMemoria = new Memoria(tamañoMemoria, tamañoKernel);
        AsignadorMemoria nuevoAsignador = new AsignadorMemoria(nuevaMemoria, tipoAsignacion, cantidadParticiones, tamañosParticion);
        Disco nuevoDisco = new Disco(tamañoDisco, tamañoIndices, tamañoDisco - tamañoMemoriaVirtual);
        CargadorArchivos nuevoCargador = new CargadorArchivos();
        List<Nucleo> nuevosNucleos = new ArrayList<>();
        for (int numero = 1; numero <= cantidadCPU; numero++) {
            nuevosNucleos.add(new Nucleo(numero, new Planificador(algoritmo, quantum, quantumsNivel, limiteEspera, tasaA, tasaB), new Despachador(new CPU())));
        }
        memoria = nuevaMemoria;
        asignador = nuevoAsignador;
        disco = nuevoDisco;
        cargador = nuevoCargador;
        nucleos = nuevosNucleos;
        procesosRegistrados = new ArrayList<>();
        gestorArchivos = new GestorArchivos(disco, procesosRegistrados);
        colaTrabajos = new ArrayDeque<>();
        reloj = 0;
        corridaIniciada = false;
        erroresPaso = new ArrayList<>();
    }
}
