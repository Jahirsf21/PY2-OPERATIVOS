package com.mycompany.t1operativos.controlador;

import com.mycompany.t1operativos.modelo.BCP;
import com.mycompany.t1operativos.modelo.Disco;
import com.mycompany.t1operativos.modelo.GestorProcesos;
import com.mycompany.t1operativos.modelo.Memoria;
import com.mycompany.t1operativos.modelo.Parser;
import com.mycompany.t1operativos.modelo.Proceso;
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
 * @author deislher sánchez funez
 */
public class Controlador {
    private Aplicacion vista;
    private GestorProcesos gestor;
    private Parser parser;
    private Timer ejecucionAutomatica;
    private boolean modoAutomatico;

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
            if (modoAutomatico) {
                ejecutarPaso();
            }
        });
        registrarEventos();
        actualizarReloj();
    }

    /**
     * Registra los eventos de la vista.
     */
    private void registrarEventos() {
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
        vista.getModeloTrabajos().addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getFirstRow() >= 0 && (e.getColumn() == 1 || e.getColumn() == 3)) {
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
            if (destino == null || destino.getProcesosRegistrados().isEmpty()) {
                destino = new GestorProcesos(vista.getMemoriaSeleccionada(), vista.getDiscoSeleccionado(), vista.getMemoriaVirtualSeleccionada(), vista.getAlgoritmoSeleccionado());
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
     * Solicita un paso y muestra su resultado.
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
        try {
            String salida = gestor.ejecutarPaso();
            if (salida != null) {
                vista.imprimirPantalla(salida);
            }
            actualizarVista();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ejecucionAutomatica.stop();
            modoAutomatico = false;
            actualizarVista();
            vista.imprimirPantalla("Error: " + ex.getMessage());
            mostrarError(ex.getMessage());
        }
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
     * @param columna 1 para llegada, 3 para prioridad.
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
            } else {
                gestor.cambiarPrioridad(idProceso, valor);
            }
        } catch (NumberFormatException ex) {
            if (columna == 1) {
                mostrarError("La llegada debe ser un entero no negativo.");
            } else {
                mostrarError("La prioridad debe ser un entero de 1 a 10.");
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
        if (gestor != null) {
            gestor.limpiar();
        }
        vista.limpiarConsola();
        vista.setTitle("Mini PC");
        actualizarVista();
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
            return;
        }
        Memoria memoria = gestor.getMemoria();
        for (int posicion = 0; posicion < memoria.getTamañoTotal(); posicion++) {
            String contenido = "";
            String[] valor = memoria.leer(posicion);
            if (valor != null) {
                if (memoria.esDireccionKernel(posicion)) {
                    contenido = valor[0] + ": " + valor[1];
                } else {
                    contenido = parser.traducirInstruccion(valor);
                }
            }
            modelo.addRow(new Object[]{posicion, contenido});
        }
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
                    modelo.addRow(new Object[]{bcp.getIdProceso(), bcp.getLlegada(), bcp.getRafaga(), bcp.getPrioridad(), bcp.getEstado()});
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
                    modelo.addRow(new Object[]{bcp.getIdProceso(), bcp.getEstado(), bcp.getRestante()});
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
        if (gestor != null) {
            reloj = gestor.getReloj();
            ocio = gestor.getSegundosOcio();
            algoritmo = gestor.getAlgoritmo();
        }
        vista.mostrarReloj("Algoritmo: " + algoritmo + "    Reloj: " + reloj + " s    Ocio CPU: " + ocio + " s");
    }

    /**
     * Muestra los tiempos simulados de todos los procesos registrados.
     */
    private void actualizarEstadisticas() {
        DefaultTableModel modelo = vista.getModeloEstadisticas();
        modelo.setRowCount(0);
        if (gestor != null) {
            for (Proceso proceso : gestor.getProcesosRegistrados()) {
                BCP bcp = proceso.getBCP();
                String retorno = "-";
                String razon = "-";
                if (bcp.getTiempoFinal() >= 0) {
                    int tr = bcp.getTiempoFinal() - bcp.getLlegada();
                    retorno = String.valueOf(tr);
                    if (bcp.getTiempoEmpleadoSegundos() > 0) {
                        razon = String.format("%.2f", (double) tr / bcp.getTiempoEmpleadoSegundos());
                    }
                }
                modelo.addRow(new Object[]{bcp.getIdProceso(), bcp.getLlegada(), bcp.getRafaga(),
                    formatearInstante(bcp.getTiempoInicio()), formatearInstante(bcp.getTiempoFinal()),
                    bcp.getTiempoEmpleadoSegundos(), retorno, razon});
            }
        }
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
        String texto = String.valueOf(instante);
        return texto;
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
        if (modoAutomatico && ejecutable) {
            if (!ejecucionAutomatica.isRunning()) {
                ejecucionAutomatica.start();
            }
        } else {
            ejecucionAutomatica.stop();
        }
        vista.getBtnEjecutar().setEnabled(ejecutable && !modoAutomatico);
        vista.getBtnPasoAPaso().setEnabled(ejecutable && !modoAutomatico);
        vista.getBtnCargarArchivo().setEnabled(true);
        vista.getBtnLimpiar().setEnabled(hayProcesosRegistrados && (!modoAutomatico || !ejecutable));
        vista.setConfiguracionHabilitada(!hayProcesosRegistrados);
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
