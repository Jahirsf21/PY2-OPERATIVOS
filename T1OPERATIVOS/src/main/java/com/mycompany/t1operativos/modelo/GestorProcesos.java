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
 * @author deislher sánchez funez
 */
public class GestorProcesos {
    private Memoria memoria;
    private Disco disco;
    private CargadorArchivos cargador;
    private GestorArchivos gestorArchivos;
    private Planificador planificador;
    private Despachador despachador;
    private List<Proceso> procesosRegistrados;
    private Queue<Proceso> colaTrabajos;
    private int reloj;
    private int segundosOcio;
    private boolean corridaIniciada;

    /**
     * Construye los recursos de la simulación.
     *
     * @param tamañoMemoria posiciones de RAM.
     * @param tamañoDisco posiciones de disco.
     * @param tamañoMemoriaVirtual posiciones reservadas en disco.
     */
    public GestorProcesos(int tamañoMemoria, int tamañoDisco, int tamañoMemoriaVirtual) {
        reiniciarRecursos(tamañoMemoria, tamañoDisco, tamañoMemoriaVirtual);
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
        if (!puedeEjecutar()) {
            throw new IllegalStateException("No hay procesos ejecutables.");
        }
        corridaIniciada = true;
        admitirLlegadas();
        if (getActual() == null && planificador.hayProcesosListos()) {
            despachador.despachar(planificador.obtenerSiguiente(), reloj);
        }
        if (getActual() == null) {
            segundosOcio++;
            reloj++;
            return null;
        }
        Proceso proceso = getActual();
        BCP bcp = proceso.getBCP();
        CPU cpu = getCPU();
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
            reloj++;
            String salida = ejecutarInstruccion(instruccion);
            bcp.guardarContexto(cpu);
            if ("TERMINADO".equals(bcp.getEstado()) || !tieneInstrucciones(bcp)) {
                finalizarProceso(proceso);
            } else {
                escribirBCPEnMemoria(proceso);
            }
            return salida;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            if (proceso == getActual()) {
                finalizarProceso(proceso);
            }
            throw ex;
        }
    }

    /**
     * Reinicia la simulación conservando capacidades.
     */
    public void limpiar() {
        reiniciarRecursos(memoria.getTamañoTotal(), disco.getTamañoTotal(), disco.getTamañoTotal() - disco.getInicioMemoriaVirtual());
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
     * Obtiene el disco activo.
     *
     * @return disco de la simulación.
     */
    public Disco getDisco() {
        return disco;
    }

    /**
     * Obtiene la CPU compartida.
     *
     * @return CPU del despachador.
     */
    public CPU getCPU() {
        return despachador.getCPU();
    }

    /**
     * Obtiene el proceso que tiene CPU.
     *
     * @return proceso actual, o null.
     */
    public Proceso getActual() {
        return despachador.getActual();
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
     * Obtiene el reloj simulado.
     *
     * @return segundos transcurridos desde cero.
     */
    public int getReloj() {
        return reloj;
    }

    /**
     * Obtiene el ocio de la CPU.
     *
     * @return segundos sin proceso en ejecución.
     */
    public int getSegundosOcio() {
        return segundosOcio;
    }

    /**
     * Comprueba si las llegadas aún se pueden editar.
     *
     * @return true antes del primer paso.
     */
    public boolean puedeEditarLlegadas() {
        return !corridaIniciada;
    }

    /**
     * Cambia la llegada de un proceso antes del primer paso.
     *
     * @param idProceso identificador del proceso.
     * @param llegada segundo de llegada, no negativo.
     */
    public void cambiarLlegada(int idProceso, int llegada) {
        if (corridaIniciada) {
            throw new IllegalStateException("Las llegadas solo se editan antes del primer paso.");
        }
        for (Proceso proceso : procesosRegistrados) {
            if (proceso.getBCP().getIdProceso() == idProceso) {
                proceso.getBCP().setLlegada(llegada);
                return;
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
        if (instrucciones.size() > memoria.getTamañoTotal() - memoria.getInicioUsuario()) {
            throw new IllegalArgumentException("El programa no cabe en el espacio de usuario.");
        }
        int sinTerminar = 0;
        for (Proceso proceso : procesosRegistrados) {
            if (!"TERMINADO".equals(proceso.getBCP().getEstado())) {
                sinTerminar++;
            }
        }
        if (sinTerminar >= 5) {
            throw new IllegalStateException("Ya hay cinco procesos no terminados.");
        }
        int id = 1;
        if (!procesosRegistrados.isEmpty()) {
            id = procesosRegistrados.get(procesosRegistrados.size() - 1).getBCP().getIdProceso() + 1;
        }
        int posicionIndice = gestorArchivos.registrarPrograma(archivo.getName(), instrucciones);
        Proceso proceso = new Proceso(new BCP(id, 1, instrucciones.size(), reloj), posicionIndice);
        procesosRegistrados.add(proceso);
    }

    /**
     * Pasa a la cola de trabajos los procesos cuya llegada ya ocurrió, por
     * llegada y luego por orden de registro, y los carga en RAM si caben.
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
        cargarPendientesEnMemoria();
    }

    /**
     * Busca un bloque libre en RAM.
     *
     * @param inicio primera posición del rango.
     * @param finExclusivo límite exclusivo del rango.
     * @param cantidad posiciones requeridas.
     * @return inicio libre, o -1.
     */
    private int buscarBloqueMemoria(int inicio, int finExclusivo, int cantidad) {
        int libres = 0;
        for (int posicion = inicio; posicion < finExclusivo; posicion++) {
            if (memoria.leer(posicion) == null) {
                libres++;
            } else {
                libres = 0;
            }
            if (libres == cantidad) {
                return posicion - cantidad + 1;
            }
        }
        return -1;
    }

    /**
     * Carga los trabajos pendientes en RAM.
     */
    private void cargarPendientesEnMemoria() {
        while (!colaTrabajos.isEmpty()) {
            Proceso proceso = colaTrabajos.peek();
            BCP bcp = proceso.getBCP();
            String[] entrada = disco.leer(proceso.getPosicionIndice());
            if (entrada == null) {
                throw new IllegalStateException("El archivo pendiente no tiene una entrada válida en disco.");
            }
            int direccion = Integer.parseInt(entrada[1]);
            int cantidad = Integer.parseInt(entrada[2]);
            int posicionBCP = buscarBloqueMemoria(0, memoria.getInicioUsuario(), getCamposBCP().length);
            int inicioPrograma = buscarBloqueMemoria(memoria.getInicioUsuario(), memoria.getTamañoTotal(), cantidad);
            if (posicionBCP < 0 || inicioPrograma < 0) {
                break;
            }
            for (int i = 0; i < cantidad; i++) {
                memoria.escribirUsuario(inicioPrograma + i, disco.leer(direccion + i));
            }
            bcp.asignarMemoria(inicioPrograma, inicioPrograma + cantidad - 1);
            proceso.asignarPosicionBCP(posicionBCP);
            bcp.setEstadoListo();
            escribirBCPEnMemoria(proceso);
            colaTrabajos.remove();
            planificador.agregarListo(proceso);
        }
        actualizarEnlacesBCP();
    }

    /**
     * Obtiene los campos del BCP.
     *
     * @return etiquetas representadas en kernel.
     */
    private String[] getCamposBCP() {
        return new String[]{"idProceso", "estado", "prioridad", "pc", "inicioMemoria", "finMemoria",
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
     * @param instruccion instrucción cargada en IR.
     * @return salida de pantalla, o null.
     */
    private String ejecutarInstruccion(String[] instruccion) {
        BCP bcp = getActual().getBCP();
        CPU cpu = getCPU();
        int siguientePc = cpu.getPc() + 1;
        String salida = null;
        switch (instruccion[0]) {
            case "JMP":
                siguientePc = calcularDestinoSalto(Integer.parseInt(instruccion[1]));
                break;
            case "JE":
                if (cpu.esIgual()) {
                    siguientePc = calcularDestinoSalto(Integer.parseInt(instruccion[1]));
                }
                break;
            case "JNE":
                if (!cpu.esIgual()) {
                    siguientePc = calcularDestinoSalto(Integer.parseInt(instruccion[1]));
                }
                break;
            case "PARAM":
                int[] valores = new int[instruccion.length - 1];
                for (int i = 0; i < valores.length; i++) {
                    valores[i] = Integer.parseInt(instruccion[i + 1]);
                }
                if (bcp.getCantidadEnPila() + valores.length > bcp.getCapacidadPila()) {
                    throw new IllegalStateException("Desbordamiento de pila: no hay espacio para los " + valores.length + " parámetros. Capacidad máxima de 5 valores.");
                }
                for (int valor : valores) {
                    bcp.apilar(valor);
                }
                break;
            case "PUSH":
                bcp.apilar(cpu.leerRegistro(instruccion[1]));
                break;
            case "POP":
                cpu.escribirRegistro(instruccion[1], bcp.desapilar());
                break;
            case "INT":
                switch (instruccion[1]) {
                    case "20H":
                        bcp.setEstadoTerminado(reloj);
                        break;
                    case "10H":
                        salida = cpu.getDx();
                        break;
                    case "21H":
                        ejecutarServicioArchivo();
                        break;
                    default:
                        throw new IllegalArgumentException("La interrupción INT " + instruccion[1] + " todavía no está implementada.");
                }
                break;
            default:
                cpu.ejecutarInstruccion(instruccion);
                break;
        }
        cpu.setPc(siguientePc);
        return salida;
    }

    /**
     * Atiende el servicio de archivos seleccionado en AH.
     */
    private void ejecutarServicioArchivo() {
        BCP bcp = getActual().getBCP();
        CPU cpu = getCPU();
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
     * Calcula la dirección de un salto.
     *
     * @param desplazamiento desplazamiento desde PC.
     * @return destino dentro del programa.
     */
    private int calcularDestinoSalto(int desplazamiento) {
        BCP bcp = getActual().getBCP();
        int destino = getCPU().getPc() + desplazamiento;
        if (destino < bcp.getInicioMemoria() || destino > bcp.getFinMemoria()) {
            throw new IllegalStateException("Salto fuera de los límites del programa: dirección " + destino + ". Rango permitido: " + bcp.getInicioMemoria() + " a " + bcp.getFinMemoria() + ".");
        }
        return destino;
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
        return bcp.getPc() <= bcp.getFinMemoria();
    }

    /**
     * Finaliza y libera un proceso.
     *
     * @param proceso proceso que termina.
     */
    private void finalizarProceso(Proceso proceso) {
        BCP bcp = proceso.getBCP();
        boolean esActual = proceso == getActual();
        int posicion = proceso.getPosicionBCP();
        int cantidadCampos = getCamposBCP().length;
        if (posicion < 0) {
            throw new IllegalStateException("El proceso no tiene memoria asignada para liberar.");
        }
        bcp.setEstadoTerminado(reloj);
        gestorArchivos.cerrarArchivos(bcp);
        if (esActual) {
            despachador.retirarActual();
        }
        memoria.liberarUsuario(bcp.getInicioMemoria(), bcp.getAlcance());
        memoria.liberarKernel(posicion, cantidadCampos);
        proceso.liberarPosicionBCP();
        bcp.setDireccionSiguienteBCP(-1);
        actualizarEnlacesBCP();
        cargarPendientesEnMemoria();
    }

    /**
     * Reinicia los recursos.
     *
     * @param tamañoMemoria posiciones de RAM.
     * @param tamañoDisco posiciones de disco.
     * @param tamañoMemoriaVirtual posiciones reservadas en disco.
     */
    private void reiniciarRecursos(int tamañoMemoria, int tamañoDisco, int tamañoMemoriaVirtual) {
        if (tamañoMemoria < 128) {
            throw new IllegalArgumentException("La RAM debe ser al menos 128 posiciones.");
        }
        int tamañoKernel = tamañoMemoria / 4;
        int tamañoIndices = tamañoDisco / 20;
        if (tamañoMemoriaVirtual < 0 || tamañoMemoriaVirtual > tamañoDisco - tamañoIndices) {
            throw new IllegalArgumentException("La memoria virtual debe estar entre 0 y " + (tamañoDisco - tamañoIndices) + " posiciones.");
        }
        Memoria nuevaMemoria = new Memoria(tamañoMemoria, tamañoKernel);
        Disco nuevoDisco = new Disco(tamañoDisco, tamañoIndices, tamañoDisco - tamañoMemoriaVirtual);
        CargadorArchivos nuevoCargador = new CargadorArchivos();
        Planificador nuevoPlanificador = new Planificador();
        Despachador nuevoDespachador = new Despachador(new CPU());
        memoria = nuevaMemoria;
        disco = nuevoDisco;
        cargador = nuevoCargador;
        planificador = nuevoPlanificador;
        despachador = nuevoDespachador;
        procesosRegistrados = new ArrayList<>();
        gestorArchivos = new GestorArchivos(disco, procesosRegistrados);
        colaTrabajos = new ArrayDeque<>();
        reloj = 0;
        segundosOcio = 0;
        corridaIniciada = false;
    }
}
