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
    private int quantumUsado;

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
     */
    public GestorProcesos(int tamañoMemoria, int tamañoDisco, int tamañoMemoriaVirtual, String algoritmo, int quantum, int[] quantumsNivel, int limiteEspera, int tasaA, int tasaB) {
        reiniciarRecursos(tamañoMemoria, tamañoDisco, tamañoMemoriaVirtual, algoritmo, quantum, quantumsNivel, limiteEspera, tasaA, tasaB);
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
        return completarPaso();
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
        boolean agotado = getActual() != null && planificador.agotoQuantum(getActual(), quantumUsado);
        if (agotado) {
            planificador.bajarNivel(getActual());
        }
        if (agotado && planificador.hayAceptadosEsperando()) {
            expropiarActual();
            cambio = true;
            agotado = false;
        }
        if (planificador.subirNivel()) {
            cambio = true;
        }
        admitirLlegadas();
        if (admitirEnMemoria()) {
            cambio = true;
        }
        if (planificador.aceptarNuevos(getActual())) {
            cambio = true;
        }
        if (agotado) {
            expropiarActual();
            cambio = true;
        } else if (planificador.debeExpropiar(getActual(), reloj)) {
            interrumpirActual();
            cambio = true;
        }
        return cambio;
    }

    /**
     * Entrega la CPU libre al siguiente proceso listo.
     *
     * @return true si despachó un proceso.
     */
    public boolean despacharPaso() {
        if (getActual() == null && planificador.hayProcesosListos()) {
            Proceso siguiente = planificador.obtenerSiguiente(reloj);
            despachador.despachar(siguiente, reloj);
            quantumUsado = siguiente.getQuantumUsado();
            siguiente.setQuantumUsado(0);
            siguiente.setTiempoEspera(0);
            return true;
        }
        return false;
    }

    /**
     * Ejecuta una instrucción del actual, o registra ocio, y avanza el reloj.
     *
     * @return salida de INT 10H, o null si no hubo salida.
     */
    public String completarPaso() {
        if (!puedeEjecutar()) {
            throw new IllegalStateException("No hay procesos ejecutables.");
        }
        planificador.contarEspera();
        planificador.aumentarPrioridades(getActual());
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
            quantumUsado++;
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
        reiniciarRecursos(memoria.getTamañoTotal(), disco.getTamañoTotal(), disco.getTamañoTotal() - disco.getInicioMemoriaVirtual(), planificador.getAlgoritmo(), planificador.getQuantum(), planificador.getQuantumsNivel(), planificador.getLimiteEspera(), planificador.getTasaA(), planificador.getTasaB());
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
     * Obtiene el algoritmo de planificación.
     *
     * @return nombre del algoritmo.
     */
    public String getAlgoritmo() {
        return planificador.getAlgoritmo();
    }

    /**
     * Obtiene el quantum de planificación.
     *
     * @return segundos por turno.
     */
    public int getQuantum() {
        return planificador.getQuantum();
    }

    /**
     * Obtiene el quantum de cada nivel de MLFQ.
     *
     * @return quantums por nivel.
     */
    public int[] getQuantumsNivel() {
        return planificador.getQuantumsNivel();
    }

    /**
     * Obtiene los segundos de espera para subir de nivel en MLFQ.
     *
     * @return límite de espera.
     */
    public int getLimiteEspera() {
        return planificador.getLimiteEspera();
    }

    /**
     * Obtiene el aumento de prioridad por segundo de los nuevos en SRR.
     *
     * @return tasa A.
     */
    public int getTasaA() {
        return planificador.getTasaA();
    }

    /**
     * Obtiene el aumento de prioridad por segundo de los aceptados en SRR.
     *
     * @return tasa B.
     */
    public int getTasaB() {
        return planificador.getTasaB();
    }

    /**
     * Obtiene los segundos del quantum de su nivel que lleva usados un proceso.
     *
     * @param proceso proceso cargado en RAM.
     * @return segundos usados; del turno en curso si tiene la CPU.
     */
    public int getQuantumUsado(Proceso proceso) {
        if (proceso == getActual()) {
            return quantumUsado;
        }
        return proceso.getQuantumUsado();
    }

    /**
     * Obtiene el resultado del último sorteo de Lottery.
     *
     * @return boleto, total y ganador, o null si no hubo sorteo.
     */
    public String getUltimoSorteo() {
        return planificador.getUltimoSorteo();
    }

    /**
     * Obtiene la razón de uso de un proceso en el reloj actual.
     *
     * @param proceso proceso cargado en RAM.
     * @return servicio / ((reloj − llegada) / n), o 0 si acaba de llegar.
     */
    public double getRazon(Proceso proceso) {
        int participantes = planificador.getListos().size();
        if (getActual() != null) {
            participantes++;
        }
        return planificador.calcularRazonGS(proceso.getBCP(), reloj, participantes);
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
     * @param instruccion instrucción cargada en IR.
     * @return salida de pantalla, o null.
     */
    private String ejecutarInstruccion(String[] instruccion) {
        CPU cpu = getCPU();
        cpu.ejecutarInstruccion(instruccion);
        String salida = null;
        if (cpu.getInterrupcion() != null) {
            salida = atenderInterrupcion(cpu.getInterrupcion());
        }
        return salida;
    }

    /**
     * Atiende la interrupción generada por la CPU.
     *
     * @param codigo código de la interrupción.
     * @return salida de pantalla, o null.
     */
    private String atenderInterrupcion(String codigo) {
        String salida = null;
        switch (codigo) {
            case "20H":
                getActual().getBCP().setEstadoTerminado(reloj);
                break;
            case "10H":
                salida = getCPU().getDx();
                break;
            case "21H":
                ejecutarServicioArchivo();
                break;
            default:
                throw new IllegalArgumentException("La interrupción INT " + codigo + " todavía no está implementada.");
        }
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
     * Expropia al proceso actual: vuelve a LISTO con su contexto guardado,
     * conserva su memoria y regresa a la cola de listos.
     */
    private void expropiarActual() {
        Proceso expropiado = despachador.expropiarActual();
        escribirBCPEnMemoria(expropiado);
        planificador.agregarListo(expropiado);
    }

    /**
     * Interrumpe al actual porque otro listo tiene preferencia y lo devuelve a listos.
     */
    private void interrumpirActual() {
        Proceso interrumpido = despachador.expropiarActual();
        escribirBCPEnMemoria(interrumpido);
        planificador.reinsertarInterrumpido(interrumpido, quantumUsado);
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
     */
    private void reiniciarRecursos(int tamañoMemoria, int tamañoDisco, int tamañoMemoriaVirtual, String algoritmo, int quantum, int[] quantumsNivel, int limiteEspera, int tasaA, int tasaB) {
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
        Planificador nuevoPlanificador = new Planificador(algoritmo, quantum, quantumsNivel, limiteEspera, tasaA, tasaB);
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
        quantumUsado = 0;
    }
}
