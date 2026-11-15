package com.mycompany.t1operativos.controlador;

import com.mycompany.t1operativos.modelo.AsignadorMemoria;
import com.mycompany.t1operativos.modelo.BCP;
import com.mycompany.t1operativos.modelo.Disco;
import com.mycompany.t1operativos.modelo.GestorProcesos;
import com.mycompany.t1operativos.modelo.Memoria;
import com.mycompany.t1operativos.modelo.Parser;
import com.mycompany.t1operativos.modelo.Planificador;
import com.mycompany.t1operativos.modelo.Proceso;
import com.mycompany.t1operativos.modelo.ResultadoCorrida;
import com.mycompany.t1operativos.vista.Aplicacion;
import java.io.File;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

/**
 * Conecta la vista con el gestor de procesos.
 *
 * @author Deislher Sánchez Funez
 * @author Natalia Granados Rosales
 */
public class Controlador {
    private Aplicacion vista;
    private GestorProcesos gestor;
    private Parser parser;
    private Timer ejecucionAutomatica;
    private boolean modoAutomatico;
    private Timer pausaFase;
    private int faseSiguiente;
    private boolean pasoEnCurso;

    /**
     * Construye el controlador y registra los eventos.
     *
     * @param vista ventana principal.
     */
    public Controlador(Aplicacion vista) {
        if (vista == null) {
            throw new IllegalArgumentException("La vista no puede ser nula.");
        }
        this.vista = vista;
        this.gestor = null;
        this.parser = new Parser();
        this.modoAutomatico = false;
        this.ejecucionAutomatica = new Timer(1000, e -> {
            if (modoAutomatico && !pasoEnCurso) {
                ejecutarPaso();
            }
        });
        this.faseSiguiente = 0;
        this.pasoEnCurso = false;
        this.pausaFase = new Timer(1000, e -> {
            if (faseSiguiente == 2) {
                despacharFase();
            } else if (faseSiguiente == 3) {
                completarFase();
            }
        });
        this.pausaFase.setRepeats(false);
        registrarEventos();
        actualizarReloj();
    }

    /**
     * Registra los eventos de la vista.
     */
    private void registrarEventos() {
        vista.addPropertyChangeListener("Configuración confirmada", evento -> actualizarVista());
        vista.addPropertyChangeListener("Archivos cargados", evento -> {
            if (!(evento.getNewValue() instanceof List<?> seleccion)) {
                mostrarError("La selección debe contener una lista de archivos.");
                return;
            }
            for (Object elemento : seleccion) {
                if (elemento != null && !(elemento instanceof File)) {
                    mostrarError("La selección contiene un elemento que no es un archivo.");
                    return;
                }
            }
            @SuppressWarnings("unchecked")
            List<File> archivos = (List<File>) seleccion;
            cargarProgramas(archivos);
        });
        vista.getBtnPasoAPaso().addActionListener(e -> {
            if (!modoAutomatico) {
                ejecutarPaso();
            }
        });
        vista.getBtnEjecutar().addActionListener(e -> ejecutarTodo());
        vista.getBtnLimpiar().addActionListener(e -> limpiar());
        vista.getBtnNuevaCorrida().addActionListener(e -> nuevaCorrida());
        vista.getModeloTrabajos().addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getFirstRow() >= 0 && (e.getColumn() == 1 || e.getColumn() == 3 || e.getColumn() == 4)) {
                cambiarDatoTrabajo(e.getFirstRow(), e.getColumn());
            }
        });
    }

    /**
     * Entrega los archivos al gestor.
     *
     * @param archivos selección de la vista.
     */
    private void cargarProgramas(List<File> archivos) {
        GestorProcesos destino = gestor;
        try {
            if (destino == null || (destino.getProcesosRegistrados().isEmpty() && destino.getHistorial().isEmpty())) {
                destino = new GestorProcesos(vista.getMemoriaSeleccionada(), vista.getDiscoSeleccionado(), vista.getMemoriaVirtualSeleccionada(), vista.getAlgoritmoSeleccionado(), vista.getQuantumSeleccionado(), vista.getQuantumsNivelSeleccionados(), vista.getLimiteEsperaSeleccionado(), vista.getTasaASeleccionada(), vista.getTasaBSeleccionada(), vista.getCantidadCPUSeleccionada(), vista.getTipoAsignacionSeleccionado(), vista.getCantidadParticionesSeleccionada(), vista.getTamañosParticionSeleccionados());
            }
            int cantidadAnterior = destino.getProcesosRegistrados().size();
            List<String> mensajes = destino.cargarProgramas(archivos);
            if (destino.getProcesosRegistrados().size() > cantidadAnterior) {
                gestor = destino;
                actualizarVista();
            }
            if (!mensajes.isEmpty()) {
                String texto = "";
                for (int i = 0; i < mensajes.size(); i++) {
                    if (i > 0) {
                        texto = texto + "\n";
                    }
                    texto = texto + mensajes.get(i);
                }
                mostrarError(texto);
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            if (destino != null && !destino.getProcesosRegistrados().isEmpty()) {
                gestor = destino;
            }
            ejecucionAutomatica.stop();
            modoAutomatico = false;
            actualizarVista();
            vista.imprimirPantalla("Error de carga: " + ex.getMessage());
            mostrarError(ex.getMessage());
        }
    }

    /**
     * Inicia un paso y admite las llegadas.
     */
    private void ejecutarPaso() {
        if (gestor == null) {
            mostrarError("Primero debe cargar programas.");
            return;
        }
        if (!gestor.puedeEjecutar()) {
            actualizarVista();
            return;
        }
        pasoEnCurso = true;
        try {
            if (gestor.admitirPaso()) {
                pausarAntesDe(2);
                return;
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            terminarPasoConError(ex);
            return;
        }
        despacharFase();
    }

    /**
     * Despacha al siguiente proceso listo.
     */
    private void despacharFase() {
        try {
            if (gestor.despacharPaso()) {
                pausarAntesDe(3);
                return;
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            terminarPasoConError(ex);
            return;
        }
        completarFase();
    }

    /**
     * Ejecuta la instrucción del paso y muestra su resultado.
     */
    private void completarFase() {
        try {
            String salida = gestor.completarPaso();
            pasoEnCurso = false;
            if (salida != null) {
                vista.imprimirPantalla(salida);
            }
            actualizarVista();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            terminarPasoConError(ex);
        }
    }

    /**
     * Muestra la vista y programa la fase siguiente.
     *
     * @param fase 2 para despacho, 3 para ejecución.
     */
    private void pausarAntesDe(int fase) {
        faseSiguiente = fase;
        actualizarVista();
        pausaFase.restart();
    }

    /**
     * Detiene el paso y muestra el error.
     *
     * @param ex error del gestor.
     */
    private void terminarPasoConError(RuntimeException ex) {
        pausaFase.stop();
        pasoEnCurso = false;
        ejecucionAutomatica.stop();
        modoAutomatico = false;
        actualizarVista();
        vista.imprimirPantalla("Error: " + ex.getMessage());
        mostrarError(ex.getMessage());
    }

    /**
     * Inicia el modo automático.
     */
    private void ejecutarTodo() {
        if (gestor == null) {
            mostrarError("Primero debe cargar programas.");
            return;
        }
        if (!gestor.puedeEjecutar()) {
            actualizarVista();
            return;
        }
        modoAutomatico = true;
        actualizarControles();
    }

    /**
     * Entrega al gestor la llegada o prioridad editada en la lista de trabajos.
     *
     * @param fila fila editada.
     * @param columna 1 para llegada, 3 para prioridad, 4 para boletos.
     */
    private void cambiarDatoTrabajo(int fila, int columna) {
        if (gestor == null) {
            return;
        }
        DefaultTableModel modelo = vista.getModeloTrabajos();
        int idProceso = Integer.parseInt(String.valueOf(modelo.getValueAt(fila, 0)));
        try {
            int valor = Integer.parseInt(String.valueOf(modelo.getValueAt(fila, columna)).trim());
            if (columna == 1) {
                gestor.cambiarLlegada(idProceso, valor);
            } else if (columna == 3) {
                gestor.cambiarPrioridad(idProceso, valor);
            } else {
                gestor.cambiarBoletos(idProceso, valor);
            }
        } catch (NumberFormatException ex) {
            if (columna == 1) {
                mostrarError("La llegada debe ser un entero no negativo.");
            } else if (columna == 3) {
                mostrarError("La prioridad debe ser un entero de 1 a 10.");
            } else {
                mostrarError("Los boletos deben ser un entero de 1 a 100.");
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mostrarError(ex.getMessage());
        }
        SwingUtilities.invokeLater(() -> actualizarVista());
    }

    /**
     * Limpia el dominio y la presentación.
     */
    private void limpiar() {
        ejecucionAutomatica.stop();
        modoAutomatico = false;
        pausaFase.stop();
        pasoEnCurso = false;
        if (gestor != null) {
            gestor.limpiar();
        }
        vista.limpiarConsola();
        vista.setTitle("Mini PC");
        actualizarVista();
    }

    /**
     * Inicia otra corrida con los mismos programas y la configuración elegida.
     */
    private void nuevaCorrida() {
        try {
            List<String> mensajes = gestor.nuevaCorrida(vista.getMemoriaSeleccionada(), vista.getDiscoSeleccionado(), vista.getMemoriaVirtualSeleccionada(), vista.getAlgoritmoSeleccionado(), vista.getQuantumSeleccionado(), vista.getQuantumsNivelSeleccionados(), vista.getLimiteEsperaSeleccionado(), vista.getTasaASeleccionada(), vista.getTasaBSeleccionada(), vista.getCantidadCPUSeleccionada(), vista.getTipoAsignacionSeleccionado(), vista.getCantidadParticionesSeleccionada(), vista.getTamañosParticionSeleccionados());
            vista.limpiarConsola();
            actualizarVista();
            if (!mensajes.isEmpty()) {
                String texto = "";
                for (int i = 0; i < mensajes.size(); i++) {
                    if (i > 0) {
                        texto = texto + "\n";
                    }
                    texto = texto + mensajes.get(i);
                }
                mostrarError(texto);
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            actualizarVista();
            mostrarError(ex.getMessage());
        }
    }

    /**
     * Actualiza los datos de la vista.
     */
    private void actualizarVista() {
        actualizarReloj();
        actualizarTablaMemoria();
        actualizarTablaDisco();
        actualizarTablaTrabajos();
        actualizarTablaProcesos();
        actualizarEstadisticas();
        actualizarBCP();
        actualizarSeleccionProximaInstruccion();
        actualizarControles();
    }

    /**
     * Muestra el contenido de RAM.
     */
    private void actualizarTablaMemoria() {
        DefaultTableModel modelo = vista.getModeloMemoria();
        modelo.setRowCount(0);
        if (gestor == null || gestor.getProcesosRegistrados().isEmpty()) {
            vista.mostrarResumenMemoria("Asignación: " + vista.getTipoAsignacionSeleccionado());
            return;
        }
        Memoria memoria = gestor.getMemoria();
        AsignadorMemoria asignador = gestor.getAsignador();
        for (int posicion = 0; posicion < memoria.getTamañoTotal(); posicion++) {
            String contenido = "";
            String particion = "";
            String[] valor = memoria.leer(posicion);
            if (valor != null) {
                if (memoria.esDireccionKernel(posicion)) {
                    contenido = valor[0] + ": " + valor[1];
                } else {
                    contenido = parser.traducirInstruccion(valor);
                }
            }
            int indice = asignador.getParticion(posicion);
            if (indice >= 0) {
                particion = "P" + indice;
            }
            modelo.addRow(new Object[]{posicion, contenido, particion});
        }
        vista.mostrarResumenMemoria(resumirMemoria(memoria, asignador));
    }

    /**
     * Arma el resumen de la zona de usuario: particiones ocupadas y fragmentación interna, o huecos libres.
     *
     * @param memoria RAM de la simulación.
     * @param asignador asignador de la zona de usuario.
     * @return texto del resumen.
     */
    private String resumirMemoria(Memoria memoria, AsignadorMemoria asignador) {
        String texto = "Asignación: " + asignador.getTipoAsignacion();
        int[] inicios = asignador.getDireccionesInicio();
        if (inicios.length > 0) {
            int ocupadas = 0;
            int fragmentacion = 0;
            for (int i = 0; i < inicios.length; i++) {
                if (asignador.estaOcupada(i)) {
                    ocupadas++;
                    for (int posicion = inicios[i]; posicion < inicios[i] + asignador.getTamañosParticion()[i]; posicion++) {
                        if (memoria.leer(posicion) == null) {
                            fragmentacion++;
                        }
                    }
                }
            }
            texto = texto + " · Particiones ocupadas " + ocupadas + " de " + inicios.length + " · Fragmentación interna " + fragmentacion;
        } else {
            String huecos = "";
            int cantidadHuecos = 0;
            int libres = 0;
            int tamañoHueco = 0;
            for (int posicion = memoria.getInicioUsuario(); posicion <= memoria.getTamañoTotal(); posicion++) {
                if (posicion < memoria.getTamañoTotal() && memoria.leer(posicion) == null) {
                    tamañoHueco++;
                } else if (tamañoHueco > 0) {
                    if (cantidadHuecos > 0) {
                        huecos = huecos + ", ";
                    }
                    huecos = huecos + tamañoHueco;
                    cantidadHuecos++;
                    libres = libres + tamañoHueco;
                    tamañoHueco = 0;
                }
            }
            texto = texto + " · Huecos " + cantidadHuecos + " (" + huecos + ") · Libre " + libres;
        }
        return texto;
    }

    /**
     * Muestra el contenido del disco.
     */
    private void actualizarTablaDisco() {
        DefaultTableModel modelo = vista.getModeloDisco();
        modelo.setRowCount(0);
        if (gestor == null || gestor.getProcesosRegistrados().isEmpty()) {
            return;
        }
        Disco disco = gestor.getDisco();
        for (int posicion = 0; posicion < disco.getTamañoTotal(); posicion++) {
            String contenido = "";
            String[] valor = disco.leer(posicion);
            if (disco.esDireccionIndice(posicion)) {
                if (valor == null) {
                    contenido = "Índice (libre)";
                } else {
                    contenido = "[" + valor[0] + ", " + valor[1] + ", " + valor[2] + "]";
                }
            } else if (disco.esDireccionMemoriaVirtual(posicion)) {
                contenido = "Memoria virtual (sin uso)";
            } else if (valor != null) {
                if (valor.length == 1) {
                    if (valor[0].isEmpty()) {
                        contenido = " ";
                    } else {
                        contenido = valor[0];
                    }
                } else {
                    contenido = parser.traducirInstruccion(valor);
                }
            }
            modelo.addRow(new Object[]{posicion, contenido});
        }
    }

    /**
     * Muestra los trabajos pendientes de RAM.
     */
    private void actualizarTablaTrabajos() {
        DefaultTableModel modelo = vista.getModeloTrabajos();
        modelo.setRowCount(0);
        if (gestor != null) {
            for (Proceso proceso : gestor.getProcesosRegistrados()) {
                BCP bcp = proceso.getBCP();
                if ("NUEVO".equals(bcp.getEstado())) {
                    modelo.addRow(new Object[]{bcp.getIdProceso(), bcp.getLlegada(), bcp.getRafaga(), bcp.getPrioridad(), bcp.getBoletos(), bcp.getEstado()});
                }
            }
        }
    }

    /**
     * Muestra los procesos cargados en RAM.
     */
    private void actualizarTablaProcesos() {
        DefaultTableModel modelo = vista.getModeloProcesos();
        modelo.setRowCount(0);
        if (gestor != null) {
            for (Proceso proceso : gestor.getProcesosRegistrados()) {
                if (proceso.getPosicionBCP() >= 0) {
                    BCP bcp = proceso.getBCP();
                    String razon = "";
                    if ("GuaranteedScheduling".equals(gestor.getAlgoritmo())) {
                        razon = String.format("%.2f", gestor.getRazon(proceso));
                    }
                    String nivel = "";
                    if ("MLFQ".equals(gestor.getAlgoritmo())) {
                        nivel = proceso.getNivel() + " (" + gestor.getQuantumUsado(proceso) + "/" + gestor.getQuantumsNivel()[proceso.getNivel()] + ")";
                    }
                    String srr = "";
                    if ("SRR".equals(gestor.getAlgoritmo())) {
                        if (proceso.estaAceptado()) {
                            srr = "A:" + proceso.getPrioridad();
                        } else {
                            srr = "N:" + proceso.getPrioridad();
                        }
                    }
                    modelo.addRow(new Object[]{bcp.getIdProceso(), bcp.getEstado(), bcp.getRestante(), razon, nivel, srr});
                }
            }
        }
    }

    /**
     * Muestra el reloj simulado y el ocio de la CPU.
     */
    private void actualizarReloj() {
        int reloj = 0;
        int ocio = 0;
        String algoritmo = vista.getAlgoritmoSeleccionado();
        int quantum = vista.getQuantumSeleccionado();
        int[] quantumsNivel = vista.getQuantumsNivelSeleccionados();
        int limiteEspera = vista.getLimiteEsperaSeleccionado();
        int tasaA = vista.getTasaASeleccionada();
        int tasaB = vista.getTasaBSeleccionada();
        String sorteo = null;
        if (gestor != null && !gestor.getProcesosRegistrados().isEmpty()) {
            reloj = gestor.getReloj();
            ocio = gestor.getSegundosOcio();
            algoritmo = gestor.getAlgoritmo();
            quantum = gestor.getQuantum();
            quantumsNivel = gestor.getQuantumsNivel();
            limiteEspera = gestor.getLimiteEspera();
            tasaA = gestor.getTasaA();
            tasaB = gestor.getTasaB();
            sorteo = gestor.getUltimoSorteo();
        }
        String texto = describirAlgoritmo(algoritmo, quantum, quantumsNivel, limiteEspera, tasaA, tasaB);
        if (sorteo != null) {
            texto = texto + "    Sorteo: " + sorteo;
        }
        vista.mostrarReloj("Algoritmo: " + texto + "    Reloj: " + reloj + " s    Ocio CPU: " + ocio + " s");
    }

    /**
     * Arma el nombre del algoritmo con sus parámetros.
     *
     * @param algoritmo nombre del algoritmo.
     * @param quantum segundos por turno.
     * @param quantumsNivel quantum de cada nivel de MLFQ.
     * @param limiteEspera segundos para subir de nivel en MLFQ.
     * @param tasaA tasa de los nuevos en SRR.
     * @param tasaB tasa de los aceptados en SRR.
     * @return texto como «RR (q = 2)».
     */
    private String describirAlgoritmo(String algoritmo, int quantum, int[] quantumsNivel, int limiteEspera, int tasaA, int tasaB) {
        String texto = algoritmo;
        if ("RR".equals(algoritmo) || "Lottery".equals(algoritmo)) {
            texto = texto + " (q = " + quantum + ")";
        }
        if ("MLFQ".equals(algoritmo)) {
            String niveles = "";
            for (int i = 0; i < quantumsNivel.length; i++) {
                if (i > 0) {
                    niveles = niveles + ", ";
                }
                niveles = niveles + quantumsNivel[i];
            }
            texto = texto + " (q = " + niveles + "; límite de espera " + limiteEspera + ")";
        }
        if ("SRR".equals(algoritmo)) {
            texto = texto + " (q = " + quantum + "; A = " + tasaA + ", B = " + tasaB + ")";
        }
        return texto;
    }

    /**
     * Muestra los tiempos simulados de todos los procesos registrados.
     */
    private void actualizarEstadisticas() {
        DefaultTableModel modelo = vista.getModeloEstadisticas();
        modelo.setRowCount(0);
        DefaultTableModel historial = vista.getModeloHistorial();
        historial.setRowCount(0);
        vista.mostrarResumenCorrida(" ");
        if (gestor == null) {
            return;
        }
        if (!gestor.getProcesosRegistrados().isEmpty()) {
            ResultadoCorrida actual = gestor.getCorridaActual();
            for (Proceso proceso : actual.getProcesos()) {
                BCP bcp = proceso.getBCP();
                modelo.addRow(new Object[]{bcp.getIdProceso(), bcp.getLlegada(), bcp.getRafaga(),
                    formatearInstante(bcp.getTiempoInicio()), formatearInstante(bcp.getTiempoFinal()),
                    bcp.getTiempoEmpleadoSegundos(), formatearInstante(actual.calcularRetorno(proceso)),
                    formatearDecimal(actual.calcularRazonRetorno(proceso))});
            }
            vista.mostrarResumenCorrida("Corrida " + actual.getNumero() + " · Reloj " + gestor.getReloj() + " s · Ocio CPU "
                    + actual.getSegundosOcio() + " s · Tr promedio " + formatearDecimal(actual.calcularPromedioRetorno())
                    + " · Tr/Ts promedio " + formatearDecimal(actual.calcularPromedioRazon()));
        }
        for (ResultadoCorrida corrida : gestor.getHistorial()) {
            Planificador planificador = corrida.getPlanificador();
            historial.addRow(new Object[]{corrida.getNumero(),
                describirAlgoritmo(planificador.getAlgoritmo(), planificador.getQuantum(), planificador.getQuantumsNivel(), planificador.getLimiteEspera(), planificador.getTasaA(), planificador.getTasaB()),
                corrida.getAsignador().getTipoAsignacion(), corrida.getProcesos().size(), corrida.getTiempoFinal(),
                corrida.getSegundosOcio(), formatearDecimal(corrida.calcularPromedioRetorno()),
                formatearDecimal(corrida.calcularPromedioRazon())});
        }
    }

    /**
     * Presenta un valor decimal con dos cifras.
     *
     * @param valor valor calculado, o -1.
     * @return el valor con dos decimales, o "-" si no aplica.
     */
    private String formatearDecimal(double valor) {
        if (valor < 0) {
            return "-";
        }
        return String.format("%.2f", valor);
    }

    /**
     * Presenta un instante simulado.
     *
     * @param instante segundo del reloj, o -1.
     * @return el número, o "-" si no existe.
     */
    private String formatearInstante(int instante) {
        if (instante < 0) {
            return "-";
        }
        return String.valueOf(instante);
    }

    /**
     * Muestra únicamente el BCP con CPU.
     */
    private void actualizarBCP() {
        Proceso proceso = null;
        if (gestor != null) {
            proceso = gestor.getActual();
        }
        if (proceso == null) {
            vista.mostrarBCP("");
            return;
        }
        BCP bcp = proceso.getBCP();
        String texto = "ID: " + bcp.getIdProceso()
                + "\nEstado: " + bcp.getEstado()
                + "\nPrioridad: " + bcp.getPrioridad()
                + "\nBoletos: " + bcp.getBoletos()
                + "\nPosición BCP: " + proceso.getPosicionBCP()
                + "\nInicio memoria: " + bcp.getInicioMemoria()
                + "\nFin memoria: " + bcp.getFinMemoria()
                + "\nPC: " + bcp.getPc()
                + "\nIR: " + bcp.getIrToString()
                + "\nAC: " + bcp.getAc()
                + "\nAX: " + bcp.getAx()
                + "\nBX: " + bcp.getBx()
                + "\nCX: " + bcp.getCx()
                + "\nDX: " + bcp.getDx()
                + "\nAH: " + bcp.getAh()
                + "\nAL: " + bcp.getAl()
                + "\nFlag: " + bcp.esIgual()
                + "\nPila 1: " + bcp.getValorPila(0)
                + "\nPila 2: " + bcp.getValorPila(1)
                + "\nPila 3: " + bcp.getValorPila(2)
                + "\nPila 4: " + bcp.getValorPila(3)
                + "\nPila 5: " + bcp.getValorPila(4)
                + "\nPuntero pila: " + bcp.getPunteroPila()
                + "\nCPU actual: " + bcp.getCpuActual()
                + "\nLlegada: " + bcp.getLlegada()
                + "\nRáfaga: " + bcp.getRafaga()
                + "\nRestante: " + bcp.getRestante()
                + "\nInicio ejecución: " + formatearInstante(bcp.getTiempoInicio())
                + "\nFin ejecución: " + formatearInstante(bcp.getTiempoFinal())
                + "\nServicio: " + bcp.getTiempoEmpleadoSegundos() + " s"
                + "\nSiguiente BCP: " + bcp.getDireccionSiguienteBCP()
                + "\nArchivos abiertos: " + bcp.getArchivosAbiertos();
        vista.mostrarBCP(texto);
    }

    /**
     * Resalta el PC del proceso actual.
     */
    private void actualizarSeleccionProximaInstruccion() {
        Proceso actual = null;
        if (gestor != null) {
            actual = gestor.getActual();
        }
        if (actual == null) {
            vista.limpiarSeleccionTablas();
        } else {
            vista.seleccionarProximaInstruccion(gestor.getCPU().getPc());
        }
    }

    /**
     * Ajusta los controles y la actividad del Timer.
     */
    private void actualizarControles() {
        boolean hayProcesosRegistrados = gestor != null && !gestor.getProcesosRegistrados().isEmpty();
        boolean ejecutable = hayProcesosRegistrados && gestor.puedeEjecutar();
        if (!hayProcesosRegistrados || !gestor.hayProcesosSinTerminar()) {
            modoAutomatico = false;
        }
        if (modoAutomatico && ejecutable && !pasoEnCurso) {
            if (!ejecucionAutomatica.isRunning()) {
                ejecucionAutomatica.start();
            }
        } else {
            ejecucionAutomatica.stop();
        }
        vista.getBtnEjecutar().setEnabled(ejecutable && !modoAutomatico && !pasoEnCurso);
        vista.getBtnPasoAPaso().setEnabled(ejecutable && !modoAutomatico && !pasoEnCurso);
        vista.getBtnCargarArchivo().setEnabled(!pasoEnCurso);
        vista.getBtnLimpiar().setEnabled(hayProcesosRegistrados && !pasoEnCurso && (!modoAutomatico || !ejecutable));
        boolean terminada = hayProcesosRegistrados && gestor.estaCorridaTerminada();
        vista.getBtnNuevaCorrida().setEnabled(terminada && !pasoEnCurso);
        vista.setConfiguracionHabilitada(!hayProcesosRegistrados || terminada);
        vista.setTrabajosEditables(hayProcesosRegistrados && gestor.puedeEditarTrabajos());
    }

    /**
     * Muestra un mensaje de error.
     *
     * @param mensaje descripción del error.
     */
    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(vista, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
