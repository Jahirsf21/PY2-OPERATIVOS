package com.mycompany.t1operativos.vista;

import com.mycompany.t1operativos.controlador.Controlador;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Ventana principal de Mini PC.
 *
 * @author Deislher Sánchez Funez
 * @author Natalia Granados Rosales
 */
public class Aplicacion extends JFrame {

    /** Campos del BCP que inician un nuevo grupo (se dibuja un separador antes). */
    private static final String[] INICIO_GRUPO_BCP = {"PC", "AX", "Pila 1", "CPU actual", "Siguiente BCP"};

    /**
     * Atributos del BCP que se muestran aunque no haya proceso en la CPU.
     * Coinciden con los que arma el controlador en actualizarBCP().
     */
    private static final String[] CAMPOS_BCP = {
            "ID", "Estado", "Prioridad", "Boletos", "Posición BCP", "Inicio memoria", "Fin memoria",
            "PC", "IR", "AC",
            "AX", "BX", "CX", "DX", "AH", "AL", "Flag",
            "Pila 1", "Pila 2", "Pila 3", "Pila 4", "Pila 5", "Puntero pila",
            "CPU actual", "Llegada", "Ráfaga", "Restante", "Inicio ejecución", "Fin ejecución", "Servicio",
            "Siguiente BCP", "Archivos abiertos"};

    private JButton btnEjecutar;
    private JButton btnPasoAPaso;
    private JButton btnLimpiar;
    /** Botón para repetir la corrida con otra configuración. */
    private JButton btnNuevaCorrida;
    private JButton btnCargarArchivo;
    /** Botón de estadísticas. */
    private JButton btnEstadisticas;
    /** Modal de estadísticas. */
    private JDialog ventanaEstadisticas;
    /** Botón de configuración. */
    private JButton btnConfigurarSimulador;
    /** Modal de configuración. */
    private JDialog ventanaConfiguracion;

    private JTable tablaMemoria;
    private JTable tablaDisco;
    /** Tabla de trabajos pendientes. */
    private JTable tablaTrabajos;
    /** Permite editar las columnas de llegada y prioridad. */
    private boolean trabajosEditables;
    private EstiloUI.Pildora lblReloj;
    /** Tabla de procesos cargados en RAM. */
    private JTable tablaProcesos;
    /** Tabla de tiempos por proceso. */
    private JTable tablaEstadisticas;
    /** Tabla de corridas terminadas. */
    private JTable tablaHistorial;
    /** Ocio y promedios de la corrida actual. */
    private JLabel lblResumenCorrida;
    /** Texto completo del BCP (se conserva para {@link #getAreaBCP()}). */
    private JTextArea areaBCP;
    /** Panel con una fila por campo del BCP. */
    private JPanel panelCamposBCP;
    /** Píldora con el estado del proceso actual. */
    private EstiloUI.Pildora lblEstadoBCP;
    /** Nombres de los campos que se muestran actualmente en el panel del BCP. */
    private final List<String> clavesBCP = new ArrayList<>();
    /** Etiquetas de valor de cada campo del BCP, en el mismo orden que {@link #clavesBCP}. */
    private final List<JLabel> valoresBCP = new ArrayList<>();
    private JSpinner selectorMemoria;
    private JLabel lblDistribucionMemoria;
    private JSpinner selectorDisco;
    private JLabel lblDistribucionDisco;
    /** Barras de distribución del diálogo de configuración. */
    private EstiloUI.BarraDistribucion barraMemoria;
    private EstiloUI.BarraDistribucion barraDisco;
    /** Tamaño elegido de memoria virtual. */
    private JSpinner selectorMemoriaVirtual;
    /** Algoritmo de planificación elegido. */
    private JComboBox<String> selectorAlgoritmo;
    /** Quantum elegido para RR. */
    private JSpinner selectorQuantum;
    /** Quantums por nivel de MLFQ. */
    private JTextField selectorQuantumsNivel;
    /** Segundos de espera para subir de nivel en MLFQ. */
    private JSpinner selectorLimiteEspera;
    /** Tasa de aumento de prioridad de los nuevos en SRR. */
    private JSpinner selectorTasaA;
    /** Tasa de aumento de prioridad de los aceptados en SRR. */
    private JSpinner selectorTasaB;
    /** Cantidad de CPU del simulador. */
    private JSpinner selectorCantidadCPU;
    /** Tipo de asignación de la zona de usuario. */
    private JComboBox<String> selectorTipoAsignacion;
    /** Cantidad de particiones fijas iguales. */
    private JSpinner selectorCantidadParticiones;
    /** Tamaños de las particiones fijas desiguales. */
    private JTextField selectorTamañosParticion;
    /** Resumen de la asignación de la zona de usuario. */
    private JLabel lblResumenMemoria;
    private JTextArea pantalla;
    private DefaultTableModel modeloMemoria;
    private DefaultTableModel modeloDisco;
    /** Filas de trabajos pendientes. */
    private DefaultTableModel modeloTrabajos;
    /** Filas de procesos cargados en RAM. */
    private DefaultTableModel modeloProcesos;
    /** Filas de estadísticas. */
    private DefaultTableModel modeloEstadisticas;
    private DefaultTableModel modeloHistorial;

    /**
     * Construye e inicializa la ventana principal.
     */
    public Aplicacion() {
        initComponents();
    }

    /**
     * Configura la ventana y sus componentes.
     */
    private void initComponents() {
        setTitle("Mini PC");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Ocupa toda el área útil de la pantalla (sin tapar la barra de tareas) y se abre maximizada.
        Rectangle pantallaUtil = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setBounds(pantallaUtil);
        setMinimumSize(new Dimension(Math.min(1100, pantallaUtil.width), Math.min(680, pantallaUtil.height)));
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel raiz = new JPanel(new BorderLayout(14, 14));
        raiz.setBackground(EstiloUI.FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(16, 20, 18, 20));
        setContentPane(raiz);

        add(crearPanelSuperior(), BorderLayout.NORTH);
        add(crearPanelTablas(), BorderLayout.CENTER);
        add(crearPanelBCP(), BorderLayout.EAST);
        add(crearPanelConsola(), BorderLayout.SOUTH);

        ventanaEstadisticas = new JDialog(this, "Estadísticas", true);
        ventanaEstadisticas.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        ventanaEstadisticas.setContentPane(crearPanelEstadisticas());
        ventanaEstadisticas.setSize(960, 640);
        ventanaEstadisticas.setMinimumSize(new Dimension(640, 480));

        ventanaConfiguracion = new JDialog(this, "Configurar Simulador", true);
        ventanaConfiguracion.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        ventanaConfiguracion.setContentPane(crearPanelConfiguracion());
        ventanaConfiguracion.pack();
        ventanaConfiguracion.setSize(Math.max(620, ventanaConfiguracion.getWidth()), ventanaConfiguracion.getHeight());
        ventanaConfiguracion.setMinimumSize(new Dimension(560, ventanaConfiguracion.getHeight()));
        ventanaConfiguracion.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evento) {
                confirmarConfiguracion();
            }
        });
    }

    // ================================================================
    // PANEL SUPERIOR
    // ================================================================

    /**
     * Crea el panel de controles del programa.
     *
     * @return el panel superior.
     */
    private JPanel crearPanelSuperior() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(EstiloUI.FONDO);

        JPanel panelBotonesAccion = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelBotonesAccion.setBackground(EstiloUI.FONDO);
        panelBotonesAccion.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnEjecutar = EstiloUI.crearBotonPrimario("Ejecutar", EstiloUI.PRIMARIO, EstiloUI.PRIMARIO_HOVER);
        btnPasoAPaso = EstiloUI.crearBotonSecundario("Paso a paso");
        btnLimpiar = EstiloUI.crearBotonPrimario("Limpiar", EstiloUI.PELIGRO, EstiloUI.PELIGRO_HOVER);
        btnEjecutar.setEnabled(false);
        btnPasoAPaso.setEnabled(false);
        btnLimpiar.setEnabled(false);
        panelBotonesAccion.add(btnEjecutar);
        panelBotonesAccion.add(btnPasoAPaso);
        panelBotonesAccion.add(btnLimpiar);
        btnNuevaCorrida = EstiloUI.crearBotonSecundario("Nueva corrida");
        btnNuevaCorrida.setEnabled(false);
        panelBotonesAccion.add(btnNuevaCorrida);
        btnEstadisticas = EstiloUI.crearBotonSecundario("Estadísticas");
        btnEstadisticas.addActionListener(e -> mostrarEstadisticas());
        panelBotonesAccion.add(btnEstadisticas);
        panelBotonesAccion.add(Box.createHorizontalStrut(8));
        lblReloj = EstiloUI.crearPildora("", EstiloUI.PRIMARIO_SUAVE, EstiloUI.PRIMARIO);
        lblReloj.setFont(EstiloUI.FUENTE_MONO_BOLD);
        lblReloj.setIcon(EstiloUI.iconoReloj(EstiloUI.PRIMARIO));
        lblReloj.setIconTextGap(8);
        lblReloj.setVisible(false);
        panelBotonesAccion.add(lblReloj);

        JPanel panelCargarArchivo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelCargarArchivo.setBackground(EstiloUI.FONDO);
        panelCargarArchivo.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnCargarArchivo = EstiloUI.crearBotonSecundario("Cargar archivos");
        btnCargarArchivo.addActionListener(e -> abrirSelectorArchivos());
        panelCargarArchivo.add(btnCargarArchivo);
        btnConfigurarSimulador = EstiloUI.crearBotonSecundario("Configurar Simulador");
        btnConfigurarSimulador.addActionListener(e -> mostrarConfiguracion());
        panelCargarArchivo.add(btnConfigurarSimulador);

        panel.add(panelBotonesAccion);
        panel.add(Box.createVerticalStrut(10));
        panel.add(panelCargarArchivo);

        return panel;
    }

    // ================================================================
    // CONFIGURACIÓN
    // ================================================================

    /**
     * Crea los controles de configuración.
     *
     * @return panel de tamaños y distribución.
     */
    private JPanel crearPanelConfiguracion() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(EstiloUI.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 18, 24));

        // ---- Encabezado ----
        JPanel encabezado = new JPanel();
        encabezado.setLayout(new BoxLayout(encabezado, BoxLayout.Y_AXIS));
        encabezado.setBackground(EstiloUI.FONDO);
        JLabel titulo = EstiloUI.crearTituloVentana("Configurar Simulador");
        JLabel subtitulo = EstiloUI.crearEtiquetaSecundaria(
                "Define la memoria, el disco y el algoritmo antes de cargar programas.");
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        encabezado.add(titulo);
        encabezado.add(Box.createVerticalStrut(5));
        encabezado.add(subtitulo);
        panel.add(encabezado, BorderLayout.NORTH);

        // ---- Selectores ----
        selectorMemoria = new JSpinner(new SpinnerNumberModel(256, 128, null, 4));
        selectorDisco = new JSpinner(new SpinnerNumberModel(512, 256, null, 4));
        int tamañoDisco = (Integer) selectorDisco.getValue();
        int maximoVirtual = tamañoDisco - tamañoDisco / 20;
        selectorMemoriaVirtual = new JSpinner(new SpinnerNumberModel(64, 0, maximoVirtual, 4));
        configurarSelector(selectorMemoria);
        configurarSelector(selectorDisco);
        configurarSelector(selectorMemoriaVirtual);
        EstiloUI.estilizarSpinner(selectorMemoria);
        EstiloUI.estilizarSpinner(selectorDisco);
        EstiloUI.estilizarSpinner(selectorMemoriaVirtual);
        selectorAlgoritmo = new JComboBox<>(new String[]{"FCFS", "SPN", "Prioridad", "HRRN", "SRT", "RR", "Lottery", "GuaranteedScheduling", "MLFQ", "SRR"});
        EstiloUI.estilizarCombo(selectorAlgoritmo);
        selectorAlgoritmo.setPreferredSize(new Dimension(130, 34));
        selectorQuantum = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
        EstiloUI.estilizarSpinner(selectorQuantum);
        selectorQuantum.setEnabled(false);
        selectorQuantumsNivel = new JTextField("1, 2, 4");
        selectorQuantumsNivel.setFont(EstiloUI.FUENTE_MONO);
        selectorQuantumsNivel.setForeground(EstiloUI.TEXTO);
        selectorQuantumsNivel.setPreferredSize(new Dimension(130, 34));
        selectorQuantumsNivel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(EstiloUI.BORDE),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        selectorQuantumsNivel.setEnabled(false);
        selectorLimiteEspera = new JSpinner(new SpinnerNumberModel(5, 1, null, 1));
        EstiloUI.estilizarSpinner(selectorLimiteEspera);
        selectorLimiteEspera.setEnabled(false);
        selectorTasaA = new JSpinner(new SpinnerNumberModel(2, 1, null, 1));
        EstiloUI.estilizarSpinner(selectorTasaA);
        selectorTasaA.setEnabled(false);
        selectorTasaB = new JSpinner(new SpinnerNumberModel(1, 0, null, 1));
        EstiloUI.estilizarSpinner(selectorTasaB);
        selectorTasaB.setEnabled(false);
        selectorCantidadCPU = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
        EstiloUI.estilizarSpinner(selectorCantidadCPU);
        selectorTipoAsignacion = new JComboBox<>(new String[]{"Fija iguales", "Fija desiguales", "First Fit", "Next Fit", "Best Fit"});
        EstiloUI.estilizarCombo(selectorTipoAsignacion);
        selectorTipoAsignacion.setPreferredSize(new Dimension(130, 34));
        selectorTipoAsignacion.setSelectedItem("First Fit");
        selectorCantidadParticiones = new JSpinner(new SpinnerNumberModel(4, 2, null, 1));
        EstiloUI.estilizarSpinner(selectorCantidadParticiones);
        selectorCantidadParticiones.setEnabled(false);
        selectorTamañosParticion = new JTextField("16, 32, 48, 96");
        selectorTamañosParticion.setFont(EstiloUI.FUENTE_MONO);
        selectorTamañosParticion.setForeground(EstiloUI.TEXTO);
        selectorTamañosParticion.setPreferredSize(new Dimension(130, 34));
        selectorTamañosParticion.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(EstiloUI.BORDE),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        selectorTamañosParticion.setEnabled(false);

        lblDistribucionMemoria = new JLabel();
        lblDistribucionMemoria.setFont(EstiloUI.FUENTE_ETIQUETA);
        lblDistribucionMemoria.setForeground(EstiloUI.TEXTO);
        lblDistribucionDisco = new JLabel();
        lblDistribucionDisco.setFont(EstiloUI.FUENTE_ETIQUETA);
        lblDistribucionDisco.setForeground(EstiloUI.TEXTO);
        barraMemoria = new EstiloUI.BarraDistribucion();
        barraDisco = new EstiloUI.BarraDistribucion();
        barraMemoria.setPreferredSize(new Dimension(300, 14));
        barraDisco.setPreferredSize(new Dimension(300, 14));

        // ---- Tarjeta de memoria principal ----
        JPanel tarjetaMemoria = crearTarjetaConfiguracion("Memoria principal");
        tarjetaMemoria.add(crearFilaCampo("Memoria principal (RAM)",
                "Se divide siempre 25% SO y 75% usuario.", selectorMemoria));
        agregarDistribucion(tarjetaMemoria, lblDistribucionMemoria, barraMemoria);
        tarjetaMemoria.add(Box.createVerticalStrut(10));
        tarjetaMemoria.add(crearFilaCampo("Tipo de asignación",
                "Cómo se ubican los programas en la zona de usuario.", selectorTipoAsignacion));
        tarjetaMemoria.add(Box.createVerticalStrut(10));
        tarjetaMemoria.add(crearFilaCampo("Cantidad de particiones",
                "Particiones iguales; deben dividir exacto la zona de usuario.", selectorCantidadParticiones));
        tarjetaMemoria.add(Box.createVerticalStrut(10));
        tarjetaMemoria.add(crearFilaCampo("Tamaños de particiones",
                "Particiones desiguales separadas por comas; deben sumar la zona de usuario.", selectorTamañosParticion));

        // ---- Tarjeta de almacenamiento secundario ----
        JPanel tarjetaDisco = crearTarjetaConfiguracion("Almacenamiento secundario");
        tarjetaDisco.add(crearFilaCampo("Disco",
                "Capacidad total. El índice ocupa el 5% inicial.", selectorDisco));
        tarjetaDisco.add(Box.createVerticalStrut(10));
        tarjetaDisco.add(crearFilaCampo("Memoria virtual",
                "Reserva al final del disco.", selectorMemoriaVirtual));
        agregarDistribucion(tarjetaDisco, lblDistribucionDisco, barraDisco);

        // ---- Tarjeta de planificación ----
        JPanel tarjetaPlanificacion = crearTarjetaConfiguracion("Planificación");
        tarjetaPlanificacion.add(crearFilaCampo("Cantidad de CPU",
                "De 1 a 5; cada CPU atiende hasta 5 procesos.", selectorCantidadCPU));
        tarjetaPlanificacion.add(Box.createVerticalStrut(10));
        tarjetaPlanificacion.add(crearFilaCampo("Algoritmo de planificación",
                "Política para elegir el siguiente proceso.", selectorAlgoritmo));
        tarjetaPlanificacion.add(Box.createVerticalStrut(10));
        tarjetaPlanificacion.add(crearFilaCampo("Quantum",
                "Segundos por turno en RR, Lottery y SRR.", selectorQuantum));
        tarjetaPlanificacion.add(Box.createVerticalStrut(10));
        tarjetaPlanificacion.add(crearFilaCampo("Quantums por Nivel de Multilevel Feedback Queue",
                "Quantum de cada cola separados por comas (2 a 5 niveles).", selectorQuantumsNivel));
        tarjetaPlanificacion.add(Box.createVerticalStrut(10));
        tarjetaPlanificacion.add(crearFilaCampo("Límite de espera",
                "Segundos de espera para subir de nivel en MLFQ.", selectorLimiteEspera));
        tarjetaPlanificacion.add(Box.createVerticalStrut(10));
        tarjetaPlanificacion.add(crearFilaCampo("Tasa A",
                "Aumento de prioridad por segundo de los nuevos en SRR.", selectorTasaA));
        tarjetaPlanificacion.add(Box.createVerticalStrut(10));
        tarjetaPlanificacion.add(crearFilaCampo("Tasa B",
                "Aumento de prioridad por segundo de los aceptados en SRR (menor que A).", selectorTasaB));

        JPanel cuerpo = new JPanel(new GridBagLayout());
        cuerpo.setBackground(EstiloUI.FONDO);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 0, 0, 12);
        gbc.gridx = 0;
        cuerpo.add(tarjetaMemoria, gbc);
        gbc.gridx = 1;
        cuerpo.add(tarjetaDisco, gbc);
        gbc.gridx = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        cuerpo.add(tarjetaPlanificacion, gbc);
        panel.add(cuerpo, BorderLayout.CENTER);

        selectorMemoria.addChangeListener(e -> actualizarDistribucionMemoria());
        selectorDisco.addChangeListener(e -> {
            actualizarLimiteMemoriaVirtual();
            actualizarDistribucionDisco();
        });
        selectorMemoriaVirtual.addChangeListener(e -> actualizarDistribucionDisco());
        selectorAlgoritmo.addActionListener(e -> habilitarCamposAlgoritmo(selectorAlgoritmo.isEnabled()));
        selectorTipoAsignacion.addActionListener(e -> habilitarCamposMemoria(selectorTipoAsignacion.isEnabled()));
        actualizarDistribucionMemoria();
        actualizarDistribucionDisco();

        // ---- Botones ----
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botones.setBackground(EstiloUI.FONDO);
        JButton aceptar = EstiloUI.crearBotonPrimario("Aceptar", EstiloUI.PRIMARIO, EstiloUI.PRIMARIO_HOVER);
        aceptar.addActionListener(e -> confirmarConfiguracion());
        botones.add(aceptar);
        panel.add(botones, BorderLayout.SOUTH);
        ventanaConfiguracion.getRootPane().setDefaultButton(aceptar);
        return panel;
    }

    /**
     * Crea una tarjeta del diálogo de configuración con su etiqueta de sección.
     *
     * @param seccion nombre de la sección.
     * @return tarjeta con BoxLayout vertical.
     */
    private JPanel crearTarjetaConfiguracion(String seccion) {
        JPanel tarjeta = EstiloUI.crearTarjeta();
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        JLabel etiqueta = EstiloUI.crearEtiquetaSeccion(seccion);
        etiqueta.setAlignmentX(Component.LEFT_ALIGNMENT);
        tarjeta.add(etiqueta);
        tarjeta.add(Box.createVerticalStrut(12));
        return tarjeta;
    }

    /**
     * Agrega a una tarjeta el separador, el texto y la barra de una distribución.
     */
    private void agregarDistribucion(JPanel tarjeta, JLabel texto, EstiloUI.BarraDistribucion barra) {
        JPanel separador = EstiloUI.crearSeparador();
        separador.setAlignmentX(Component.LEFT_ALIGNMENT);
        texto.setAlignmentX(Component.LEFT_ALIGNMENT);
        tarjeta.add(Box.createVerticalStrut(14));
        tarjeta.add(separador);
        tarjeta.add(Box.createVerticalStrut(12));
        tarjeta.add(barra);
        tarjeta.add(Box.createVerticalStrut(8));
        tarjeta.add(texto);
    }

    /**
     * Construye una fila de configuración: título y descripción a la izquierda,
     * campo a la derecha.
     */
    private JPanel crearFilaCampo(String titulo, String descripcion, JComponent campo) {
        JPanel fila = new JPanel(new BorderLayout(15, 0));
        fila.setBackground(EstiloUI.PANEL);
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textos = new JPanel();
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setBackground(EstiloUI.PANEL);
        JLabel etiquetaTitulo = new JLabel("<html><div style='width:170px'>" + titulo + "</div></html>");
        etiquetaTitulo.setFont(EstiloUI.FUENTE_BASE.deriveFont(Font.BOLD));
        etiquetaTitulo.setForeground(EstiloUI.TEXTO);
        JLabel etiquetaDescripcion = EstiloUI.crearEtiquetaSecundaria("<html><div style='width:170px'>" + descripcion + "</div></html>");
        etiquetaTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        etiquetaDescripcion.setAlignmentX(Component.LEFT_ALIGNMENT);
        textos.add(etiquetaTitulo);
        textos.add(Box.createVerticalStrut(3));
        textos.add(etiquetaDescripcion);

        JPanel contenedorCampo = new JPanel(new GridBagLayout());
        contenedorCampo.setBackground(EstiloUI.PANEL);
        contenedorCampo.add(campo);

        fila.add(textos, BorderLayout.CENTER);
        fila.add(contenedorCampo, BorderLayout.EAST);
        fila.setMaximumSize(new Dimension(EstiloUI.ANCHO_LIBRE, fila.getPreferredSize().height));
        return fila;
    }

    /**
     * Muestra la configuración.
     */
    private void mostrarConfiguracion() {
        ventanaConfiguracion.setLocationRelativeTo(this);
        ventanaConfiguracion.setVisible(true);
    }

    /**
     * Confirma los tamaños y cierra el modal.
     */
    private void confirmarConfiguracion() {
        try {
            getMemoriaSeleccionada();
            getDiscoSeleccionado();
            getMemoriaVirtualSeleccionada();
            getQuantumSeleccionado();
            getQuantumsNivelSeleccionados();
            getLimiteEsperaSeleccionado();
            if (getTasaBSeleccionada() >= getTasaASeleccionada()) {
                throw new IllegalArgumentException("La tasa B debe ser menor que la tasa A.");
            }
            validarParticiones();
            ventanaConfiguracion.setVisible(false);
            firePropertyChange("Configuración confirmada", null, Boolean.TRUE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(ventanaConfiguracion, ex.getMessage(), "Configuración inválida", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================================================================
    // TABLAS
    // ================================================================

    /**
     * Crea las tablas de recursos, trabajos y procesos.
     *
     * @return el panel con las tablas.
     */
    private JPanel crearPanelTablas() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(EstiloUI.FONDO);
        JPanel recursos = new JPanel(new BorderLayout(14, 0));
        recursos.setBackground(EstiloUI.FONDO);

        // ---- RAM ----
        modeloMemoria = new DefaultTableModel(new Object[]{"Posición", "Valor en memoria", "Partición"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaMemoria = new JTable(modeloMemoria);
        EstiloUI.aplicarEstiloTabla(tablaMemoria, new RendererMemoria());
        tablaMemoria.setCellSelectionEnabled(false);
        tablaMemoria.setRowSelectionAllowed(true);
        tablaMemoria.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaMemoria.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] anchosMemoria = {76, 230, 130};
        for (int i = 0; i < anchosMemoria.length; i++) {
            TableColumn columna = tablaMemoria.getColumnModel().getColumn(i);
            columna.setMinWidth(anchosMemoria[i]);
            columna.setMaxWidth(anchosMemoria[i]);
            columna.setPreferredWidth(anchosMemoria[i]);
            columna.setWidth(anchosMemoria[i]);
            columna.setResizable(false);
        }
        tablaMemoria.setPreferredScrollableViewportSize(new Dimension(tablaMemoria.getColumnModel().getTotalColumnWidth(), tablaMemoria.getPreferredScrollableViewportSize().height));
        JPanel panelMemoria = EstiloUI.crearTarjetaConTitulo("RAM", EstiloUI.iconoTarjetaRAM(),
                EstiloUI.PRIMARIO, EstiloUI.crearScroll(tablaMemoria));
        JPanel pieMemoria = new JPanel();
        pieMemoria.setLayout(new BoxLayout(pieMemoria, BoxLayout.Y_AXIS));
        pieMemoria.setOpaque(false);
        JLabel leyendaMemoria = EstiloUI.crearEtiquetaSecundaria("<html>"
                + EstiloUI.cuadroColor(EstiloUI.COLOR_KERNEL) + " Kernel &nbsp;&nbsp;"
                + EstiloUI.cuadroColor(EstiloUI.COLOR_KERNEL_BCP) + " BCP &nbsp;&nbsp;"
                + EstiloUI.cuadroColor(EstiloUI.FONDO) + " Usuario &nbsp;&nbsp;"
                + EstiloUI.cuadroColor(EstiloUI.COLOR_FRAGMENTACION) + " Frag. interna</html>");
        leyendaMemoria.setBorder(BorderFactory.createEmptyBorder(8, 2, 0, 2));
        lblResumenMemoria = EstiloUI.crearEtiquetaSecundaria("");
        lblResumenMemoria.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 2));
        pieMemoria.add(leyendaMemoria);
        pieMemoria.add(lblResumenMemoria);
        panelMemoria.add(pieMemoria, BorderLayout.SOUTH);
        recursos.add(panelMemoria, BorderLayout.WEST);

        // ---- Disco ----
        modeloDisco = new DefaultTableModel(new Object[]{"Posición", "Valor en disco"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaDisco = new JTable(modeloDisco);
        EstiloUI.aplicarEstiloTabla(tablaDisco, new RendererDisco());
        tablaDisco.setCellSelectionEnabled(false);
        tablaDisco.setRowSelectionAllowed(true);
        tablaDisco.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaDisco.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        TableColumn columnaPosicion = tablaDisco.getColumnModel().getColumn(0);
        columnaPosicion.setMinWidth(76);
        columnaPosicion.setMaxWidth(76);
        columnaPosicion.setPreferredWidth(76);
        columnaPosicion.setResizable(false);
        tablaDisco.getColumnModel().getColumn(1).setPreferredWidth(260);
        tablaDisco.setPreferredScrollableViewportSize(new Dimension(336, tablaDisco.getPreferredScrollableViewportSize().height));
        JPanel panelDisco = EstiloUI.crearTarjetaConTitulo("Disco", EstiloUI.iconoTarjetaDisco(),
                EstiloUI.ACENTO_VIOLETA, EstiloUI.crearScroll(tablaDisco));
        panelDisco.add(crearLeyenda(EstiloUI.COLOR_DISCO_INDICE, "Índice",
                EstiloUI.FONDO, "Programas y datos",
                EstiloUI.COLOR_DISCO_VIRTUAL, "Memoria virtual"), BorderLayout.SOUTH);
        recursos.add(panelDisco, BorderLayout.CENTER);
        panel.add(recursos, BorderLayout.CENTER);

        JPanel listas = new JPanel(new GridLayout(1, 2, 14, 0));
        listas.setBackground(EstiloUI.FONDO);
        listas.add(crearPanelTrabajos());
        listas.add(crearPanelProcesos());
        panel.add(listas, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Crea una leyenda de tres colores para una tabla.
     */
    private JLabel crearLeyenda(Color color1, String texto1, Color color2, String texto2, Color color3, String texto3) {
        JLabel leyenda = EstiloUI.crearEtiquetaSecundaria("<html>"
                + EstiloUI.cuadroColor(color1) + " " + texto1 + " &nbsp;&nbsp;"
                + EstiloUI.cuadroColor(color2) + " " + texto2 + " &nbsp;&nbsp;"
                + EstiloUI.cuadroColor(color3) + " " + texto3 + "</html>");
        leyenda.setBorder(BorderFactory.createEmptyBorder(8, 2, 0, 2));
        return leyenda;
    }

    /**
     * Permite escribir tamaños enteros dentro de sus límites.
     *
     * @param selector control del recurso.
     */
    private void configurarSelector(JSpinner selector) {
        JFormattedTextField campo = ((JSpinner.DefaultEditor) selector.getEditor()).getTextField();
        NumberFormatter formato = new NumberFormatter(new DecimalFormat("0")) {
            @Override
            public Object stringToValue(String texto) throws ParseException {
                int tamaño;
                try {
                    tamaño = Integer.parseInt(texto.trim());
                } catch (NumberFormatException ex) {
                    throw new ParseException("Debe ingresar un tamaño entero.", 0);
                }
                int minimo = (Integer) ((SpinnerNumberModel) selector.getModel()).getMinimum();
                if (tamaño < minimo) {
                    throw new ParseException("El tamaño debe ser al menos " + minimo + " posiciones.", 0);
                }
                Integer maximo = (Integer) ((SpinnerNumberModel) selector.getModel()).getMaximum();
                if (maximo != null && tamaño > maximo) {
                    throw new ParseException("El tamaño no puede superar " + maximo + " posiciones.", 0);
                }
                return tamaño;
            }
        };
        formato.setOverwriteMode(false);
        campo.setFormatterFactory(new DefaultFormatterFactory(formato));
        campo.setFocusLostBehavior(JFormattedTextField.COMMIT);
        campo.setEditable(true);
    }

    /**
     * Crea la tabla de trabajos pendientes.
     *
     * @return panel de trabajos y estados.
     */
    private JPanel crearPanelTrabajos() {
        modeloTrabajos = new DefaultTableModel(new Object[]{"Trabajo", "Llegada", "Ráfaga", "Prioridad", "Boletos", "CPU", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                boolean editable = (col == 1 || col == 3 || col == 4) && trabajosEditables;
                return editable;
            }
        };
        tablaTrabajos = new JTable(modeloTrabajos);
        EstiloUI.aplicarEstiloTabla(tablaTrabajos, new RendererLista());
        tablaTrabajos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configurarTablaLista(tablaTrabajos);
        return EstiloUI.crearTarjetaConTitulo("Lista de Trabajos", EstiloUI.iconoTarjetaTrabajos(),
                EstiloUI.ACENTO_AMBAR, EstiloUI.crearScroll(tablaTrabajos));
    }

    /**
     * Crea la tabla de procesos cargados en RAM.
     *
     * @return panel de procesos y estados.
     */
    private JPanel crearPanelProcesos() {
        modeloProcesos = new DefaultTableModel(new Object[]{"Proceso", "CPU", "Estado", "Restante", "Razón", "Nivel", "SRR"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaProcesos = new JTable(modeloProcesos);
        EstiloUI.aplicarEstiloTabla(tablaProcesos, new RendererLista());
        tablaProcesos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configurarTablaLista(tablaProcesos);
        return EstiloUI.crearTarjetaConTitulo("Lista de Procesos", EstiloUI.iconoTarjetaProcesos(),
                EstiloUI.ACENTO_VERDE, EstiloUI.crearScroll(tablaProcesos));
    }

    /**
     * Ajusta una lista al panel y bloquea el ajuste manual.
     *
     * @param tabla tabla de trabajos o procesos.
     */
    private void configurarTablaLista(JTable tabla) {
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setResizingAllowed(false);
        int ancho = 300 / (tabla.getColumnCount() + 1);
        for (int i = 0; i < tabla.getColumnCount(); i++) {
            TableColumn columna = tabla.getColumnModel().getColumn(i);
            int anchoColumna = ancho;
            if ("Estado".equals(tabla.getColumnName(i))) {
                anchoColumna = ancho * 2;
            }
            columna.setPreferredWidth(anchoColumna);
            columna.setWidth(anchoColumna);
            columna.setResizable(false);
        }
        tabla.setPreferredScrollableViewportSize(new Dimension(tabla.getColumnModel().getTotalColumnWidth(), 100));
    }

    // ================================================================
    // ESTADÍSTICAS
    // ================================================================

    /**
     * Crea la tabla de estadísticas.
     *
     * @return panel de tiempos por proceso.
     */
    private JPanel crearPanelEstadisticas() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(EstiloUI.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 18, 24));

        JPanel encabezado = new JPanel();
        encabezado.setLayout(new BoxLayout(encabezado, BoxLayout.Y_AXIS));
        encabezado.setBackground(EstiloUI.FONDO);
        JLabel titulo = EstiloUI.crearTituloVentana("Estadísticas");
        JLabel subtitulo = EstiloUI.crearEtiquetaSecundaria(
                "Tiempos simulados por corrida. Tr = tiempo de retorno, Ts = tiempo de servicio.");
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        encabezado.add(titulo);
        encabezado.add(Box.createVerticalStrut(5));
        encabezado.add(subtitulo);
        panel.add(encabezado, BorderLayout.NORTH);

        modeloEstadisticas = new DefaultTableModel(new Object[]{"Proceso", "Llegada", "Ráfaga", "Inicio", "Final", "Servicio (s)", "Tr", "Tr/Ts"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaEstadisticas = new JTable(modeloEstadisticas);
        EstiloUI.aplicarEstiloTabla(tablaEstadisticas);
        tablaEstadisticas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lblResumenCorrida = EstiloUI.crearEtiquetaSecundaria(" ");
        lblResumenCorrida.setBorder(BorderFactory.createEmptyBorder(8, 2, 0, 2));
        JPanel contenidoActual = new JPanel(new BorderLayout());
        contenidoActual.setOpaque(false);
        contenidoActual.add(EstiloUI.crearScroll(tablaEstadisticas), BorderLayout.CENTER);
        contenidoActual.add(lblResumenCorrida, BorderLayout.SOUTH);

        modeloHistorial = new DefaultTableModel(new Object[]{"Corrida", "Algoritmo", "Asignación", "Procesos", "Final", "Ocio (s)", "Tr promedio", "Tr/Ts promedio"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaHistorial = new JTable(modeloHistorial);
        EstiloUI.aplicarEstiloTabla(tablaHistorial);
        tablaHistorial.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel tarjetas = new JPanel(new GridLayout(2, 1, 0, 14));
        tarjetas.setOpaque(false);
        tarjetas.add(EstiloUI.crearTarjetaConTitulo("Corrida actual", EstiloUI.iconoReloj(EstiloUI.PRIMARIO),
                EstiloUI.PRIMARIO, contenidoActual));
        tarjetas.add(EstiloUI.crearTarjetaConTitulo("Historial de corridas", EstiloUI.iconoReloj(EstiloUI.PRIMARIO),
                EstiloUI.PRIMARIO, EstiloUI.crearScroll(tablaHistorial)));
        panel.add(tarjetas, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botones.setBackground(EstiloUI.FONDO);
        JButton cerrar = EstiloUI.crearBotonSecundario("Cerrar");
        cerrar.addActionListener(e -> ventanaEstadisticas.setVisible(false));
        botones.add(cerrar);
        panel.add(botones, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Muestra las estadísticas.
     */
    private void mostrarEstadisticas() {
        ventanaEstadisticas.setLocationRelativeTo(this);
        ventanaEstadisticas.setVisible(true);
    }

    // ================================================================
    // BCP
    // ================================================================

    /**
     * Crea el panel del BCP actual.
     *
     * @return el panel del BCP.
     */
    private JPanel crearPanelBCP() {
        JPanel panel = EstiloUI.crearTarjeta(EstiloUI.PRIMARIO);
        panel.setPreferredSize(new Dimension(320, 0));

        JPanel encabezado = new JPanel(new BorderLayout(8, 0));
        encabezado.setBackground(EstiloUI.PANEL);
        encabezado.setBorder(BorderFactory.createEmptyBorder(0, 2, 12, 2));
        JLabel titulo = EstiloUI.crearTitulo("BCP actual");
        titulo.setIcon(EstiloUI.iconoTarjetaBCP());
        titulo.setIconTextGap(8);
        encabezado.add(titulo, BorderLayout.WEST);
        lblEstadoBCP = EstiloUI.crearPildora("", EstiloUI.PRIMARIO_SUAVE, EstiloUI.PRIMARIO);
        lblEstadoBCP.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        lblEstadoBCP.setVisible(false);
        encabezado.add(lblEstadoBCP, BorderLayout.EAST);
        panel.add(encabezado, BorderLayout.NORTH);

        // El texto completo se conserva en un JTextArea para getAreaBCP().
        areaBCP = new JTextArea();
        areaBCP.setEditable(false);
        areaBCP.setFont(EstiloUI.FUENTE_MONO);
        areaBCP.setLineWrap(true);
        areaBCP.setWrapStyleWord(true);

        panelCamposBCP = new PanelDesplazable(new GridBagLayout());
        panelCamposBCP.setBackground(EstiloUI.PANEL);
        JScrollPane scroll = new JScrollPane(panelCamposBCP,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(EstiloUI.PANEL);
        EstiloUI.aplicarBarras(scroll, EstiloUI.PULGAR, EstiloUI.PULGAR_HOVER, EstiloUI.PANEL);
        panel.add(scroll, BorderLayout.CENTER);

        mostrarBCPVacio();
        return panel;
    }

    /**
     * Obtiene el área de texto del BCP.
     *
     * @return el área del BCP actual.
     */
    public JTextArea getAreaBCP() {
        return areaBCP;
    }

    /**
     * Actualiza la información del BCP.
     *
     * Cada línea "Campo: valor" del texto se muestra en su propia fila,
     * con el nombre a la izquierda y el valor a la derecha.
     *
     * @param textoBCP texto que se desea mostrar.
     */
    public void mostrarBCP(String textoBCP) {
        areaBCP.setText(textoBCP);
        List<String[]> campos = interpretarBCP(textoBCP);
        if (campos.isEmpty()) {
            mostrarBCPVacio();
            return;
        }
        lblEstadoBCP.setVisible(false);

        List<String> claves = new ArrayList<>();
        for (String[] campo : campos) {
            claves.add(campo[0]);
        }
        if (!claves.equals(clavesBCP)) {
            reconstruirFilasBCP(claves);
        }
        for (int i = 0; i < campos.size(); i++) {
            JLabel valor = valoresBCP.get(i);
            String texto = campos.get(i)[1];
            if (texto.isEmpty()) {
                texto = "-";
            }
            valor.setText(texto);
            valor.setToolTipText(campos.get(i)[0] + ": " + texto);
            if ("Estado".equals(campos.get(i)[0])) {
                valor.setForeground(EstiloUI.colorEstado(texto));
                actualizarEstadoBCP(texto);
            }
        }
    }

    /**
     * Separa el texto del BCP en pares nombre / valor.
     */
    private List<String[]> interpretarBCP(String textoBCP) {
        List<String[]> campos = new ArrayList<>();
        if (textoBCP == null || textoBCP.isBlank()) {
            return campos;
        }
        for (String linea : textoBCP.split("\\R")) {
            if (linea.isBlank()) {
                continue;
            }
            int separador = linea.indexOf(':');
            if (separador < 0) {
                campos.add(new String[]{linea.trim(), ""});
            } else {
                campos.add(new String[]{linea.substring(0, separador).trim(), linea.substring(separador + 1).trim()});
            }
        }
        return campos;
    }

    /**
     * Vuelve a crear las filas del BCP cuando cambian los campos.
     */
    private void reconstruirFilasBCP(List<String> claves) {
        panelCamposBCP.removeAll();
        clavesBCP.clear();
        valoresBCP.clear();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        int fila = 0;
        for (int i = 0; i < claves.size(); i++) {
            String clave = claves.get(i);
            if (i > 0 && esInicioGrupoBCP(clave)) {
                gbc.gridy = fila++;
                gbc.insets = new Insets(8, 4, 8, 4);
                panelCamposBCP.add(EstiloUI.crearSeparador(), gbc);
            }
            JPanel filaPanel = new JPanel(new BorderLayout(12, 0));
            filaPanel.setBackground(EstiloUI.PANEL);
            JLabel nombre = EstiloUI.crearEtiquetaSecundaria(clave);
            JLabel valor = new JLabel("-");
            valor.setFont(EstiloUI.FUENTE_MONO_BOLD);
            valor.setForeground(EstiloUI.TEXTO);
            valor.setHorizontalAlignment(SwingConstants.RIGHT);
            valor.setMinimumSize(new Dimension(20, valor.getPreferredSize().height));
            filaPanel.add(nombre, BorderLayout.WEST);
            filaPanel.add(valor, BorderLayout.CENTER);

            gbc.gridy = fila++;
            gbc.insets = new Insets(4, 4, 4, 4);
            panelCamposBCP.add(filaPanel, gbc);
            clavesBCP.add(clave);
            valoresBCP.add(valor);
        }
        gbc.gridy = fila;
        gbc.weighty = 1;
        panelCamposBCP.add(Box.createVerticalGlue(), gbc);
        panelCamposBCP.revalidate();
        panelCamposBCP.repaint();
    }

    /**
     * Sin proceso en la CPU: se muestran todos los atributos del BCP con "-".
     */
    private void mostrarBCPVacio() {
        lblEstadoBCP.setVisible(false);
        List<String> campos = new ArrayList<>();
        for (String campo : CAMPOS_BCP) {
            campos.add(campo);
        }
        if (!campos.equals(clavesBCP)) {
            reconstruirFilasBCP(campos);
        }
        for (int i = 0; i < valoresBCP.size(); i++) {
            JLabel valor = valoresBCP.get(i);
            valor.setText("-");
            valor.setForeground(EstiloUI.TEXTO);
            valor.setToolTipText(null);
        }
    }

    /**
     * Indica si un campo del BCP inicia un grupo (lleva separador antes).
     *
     * @param clave nombre del campo.
     * @return true si inicia un grupo.
     */
    private boolean esInicioGrupoBCP(String clave) {
        for (String inicio : INICIO_GRUPO_BCP) {
            if (inicio.equals(clave)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Actualiza la píldora de estado del encabezado del BCP.
     */
    private void actualizarEstadoBCP(String estado) {
        Color color = EstiloUI.colorEstado(estado);
        lblEstadoBCP.setText(estado);
        lblEstadoBCP.setColores(EstiloUI.suavizar(color, 0.86), color);
        lblEstadoBCP.setVisible(true);
    }

    // ================================================================
    // CONSOLA
    // ================================================================

    /**
     * Crea la consola de la aplicación.
     *
     * @return el panel de la consola.
     */
    private JPanel crearPanelConsola() {
        pantalla = new JTextArea(5, 40);
        pantalla.setEditable(false);
        pantalla.setFont(EstiloUI.FUENTE_MONO);
        pantalla.setBackground(EstiloUI.CONSOLA_FONDO);
        pantalla.setForeground(EstiloUI.CONSOLA_TEXTO);
        pantalla.setCaretColor(EstiloUI.CONSOLA_TEXTO);
        pantalla.setSelectionColor(EstiloUI.PRIMARIO);
        pantalla.setSelectedTextColor(Color.WHITE);
        pantalla.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JScrollPane scroll = new JScrollPane(pantalla);
        scroll.setBorder(BorderFactory.createLineBorder(EstiloUI.CONSOLA_FONDO));
        scroll.getViewport().setBackground(EstiloUI.CONSOLA_FONDO);
        EstiloUI.aplicarBarras(scroll, EstiloUI.CONSOLA_PULGAR, EstiloUI.CONSOLA_PULGAR_HOVER, EstiloUI.CONSOLA_FONDO);

        return EstiloUI.crearTarjetaConTitulo("Consola", EstiloUI.iconoTarjetaConsola(),
                EstiloUI.CONSOLA_FONDO, scroll);
    }

    /**
     * Obtiene la consola de la aplicación.
     *
     * @return el área de texto de la consola.
     */
    public JTextArea getPantalla() {
        return pantalla;
    }

    /**
     * Agrega una línea a la consola.
     *
     * @param texto texto que se desea mostrar.
     */
    public void imprimirPantalla(String texto) {
        pantalla.append(texto + "\n");
        pantalla.setCaretPosition(pantalla.getDocument().getLength());
    }

    /**
     * Limpia la consola.
     */
    public void limpiarConsola() {
        pantalla.setText("");
    }

    // ================================================================
    // CONFIGURACIÓN SELECCIONADA
    // ================================================================

    /**
     * Confirma y obtiene el tamaño elegido de RAM.
     *
     * @return la cantidad seleccionada de posiciones de memoria.
     */
    public int getMemoriaSeleccionada() {
        return leerTamañoSeleccionado(selectorMemoria, "RAM");
    }

    /**
     * Confirma y obtiene el tamaño elegido de disco.
     *
     * @return la cantidad seleccionada de posiciones del disco.
     */
    public int getDiscoSeleccionado() {
        return leerTamañoSeleccionado(selectorDisco, "disco");
    }

    /**
     * Confirma y obtiene la memoria virtual elegida.
     *
     * @return cantidad de posiciones reservadas.
     */
    public int getMemoriaVirtualSeleccionada() {
        return leerTamañoSeleccionado(selectorMemoriaVirtual, "memoria virtual");
    }

    /**
     * Confirma el tamaño escrito y lo devuelve.
     *
     * @param selector control del recurso.
     * @param nombre nombre del recurso.
     * @return tamaño confirmado.
     */
    private int leerTamañoSeleccionado(JSpinner selector, String nombre) {
        try {
            selector.commitEdit();
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Tamaño de " + nombre + " inválido: " + ex.getMessage());
        }
        return (Integer) selector.getValue();
    }

    /**
     * Habilita o deshabilita los controles del programa.
     *
     * @param habilitados {@code true} para habilitar los controles.
     */
    public void setControlesProgramaHabilitados(boolean habilitados) {
        btnEjecutar.setEnabled(habilitados);
        btnPasoAPaso.setEnabled(habilitados);
        btnLimpiar.setEnabled(habilitados);
    }

    /**
     * Habilita o deshabilita la configuración.
     *
     * @param habilitado {@code true} para permitir cambiar el tamaño.
     */
    public void setConfiguracionHabilitada(boolean habilitado) {
        btnConfigurarSimulador.setEnabled(habilitado);
        selectorMemoria.setEnabled(habilitado);
        selectorDisco.setEnabled(habilitado);
        selectorMemoriaVirtual.setEnabled(habilitado);
        selectorCantidadCPU.setEnabled(habilitado);
        selectorAlgoritmo.setEnabled(habilitado);
        habilitarCamposAlgoritmo(habilitado);
        selectorTipoAsignacion.setEnabled(habilitado);
        habilitarCamposMemoria(habilitado);
    }

    /**
     * Habilita los parámetros del tipo de asignación elegido.
     *
     * @param habilitado {@code true} si la configuración se puede cambiar.
     */
    private void habilitarCamposMemoria(boolean habilitado) {
        String tipo = getTipoAsignacionSeleccionado();
        selectorCantidadParticiones.setEnabled(habilitado && "Fija iguales".equals(tipo));
        selectorTamañosParticion.setEnabled(habilitado && "Fija desiguales".equals(tipo));
    }

    /**
     * Valida que las particiones fijas ocupen exacto la zona de usuario de la RAM elegida.
     */
    private void validarParticiones() {
        int zonaUsuario = getMemoriaSeleccionada() - getMemoriaSeleccionada() / 4;
        String tipo = getTipoAsignacionSeleccionado();
        if ("Fija iguales".equals(tipo)) {
            int cantidad = getCantidadParticionesSeleccionada();
            if (cantidad < 2 || cantidad > zonaUsuario) {
                throw new IllegalArgumentException("La cantidad de particiones debe estar entre 2 y " + zonaUsuario + ".");
            }
            if (zonaUsuario % cantidad != 0) {
                throw new IllegalArgumentException("La zona de usuario tiene " + zonaUsuario + " posiciones y no se divide exacto en " + cantidad + " particiones.");
            }
        } else if ("Fija desiguales".equals(tipo)) {
            int[] tamaños = getTamañosParticionSeleccionados();
            if (tamaños.length < 2) {
                throw new IllegalArgumentException("Debe haber al menos 2 particiones desiguales.");
            }
            int suma = 0;
            for (int tamaño : tamaños) {
                if (tamaño < 1) {
                    throw new IllegalArgumentException("El tamaño de cada partición debe ser un entero mayor o igual a 1.");
                }
                suma = suma + tamaño;
            }
            if (suma != zonaUsuario) {
                throw new IllegalArgumentException("Las particiones suman " + suma + " y deben sumar exacto la zona de usuario de " + zonaUsuario + " posiciones.");
            }
        }
    }

    /**
     * Habilita los campos que usa el algoritmo elegido.
     *
     * @param habilitado {@code true} si la configuración se puede cambiar.
     */
    private void habilitarCamposAlgoritmo(boolean habilitado) {
        boolean esMLFQ = "MLFQ".equals(getAlgoritmoSeleccionado());
        selectorQuantum.setEnabled(habilitado && usaQuantum());
        selectorQuantumsNivel.setEnabled(habilitado && esMLFQ);
        selectorLimiteEspera.setEnabled(habilitado && esMLFQ);
        boolean esSRR = "SRR".equals(getAlgoritmoSeleccionado());
        selectorTasaA.setEnabled(habilitado && esSRR);
        selectorTasaB.setEnabled(habilitado && esSRR);
    }

    /**
     * Indica si el algoritmo elegido usa quantum.
     *
     * @return true para RR, Lottery y SRR.
     */
    private boolean usaQuantum() {
        String algoritmo = getAlgoritmoSeleccionado();
        boolean usa = "RR".equals(algoritmo) || "Lottery".equals(algoritmo) || "SRR".equals(algoritmo);
        return usa;
    }

    /**
     * Obtiene el algoritmo elegido.
     *
     * @return nombre del algoritmo.
     */
    public String getAlgoritmoSeleccionado() {
        return (String) selectorAlgoritmo.getSelectedItem();
    }

    /**
     * Confirma y obtiene el quantum elegido.
     *
     * @return segundos por turno.
     */
    public int getQuantumSeleccionado() {
        try {
            selectorQuantum.commitEdit();
        } catch (ParseException ex) {
            throw new IllegalArgumentException("El quantum debe ser un entero mayor o igual a 1.");
        }
        return (Integer) selectorQuantum.getValue();
    }

    /**
     * Confirma y obtiene el quantum de cada nivel de MLFQ.
     *
     * @return quantums por nivel.
     */
    public int[] getQuantumsNivelSeleccionados() {
        String[] partes = selectorQuantumsNivel.getText().split(",");
        if (partes.length < 2 || partes.length > 5) {
            throw new IllegalArgumentException("Los quantums por nivel deben ser de 2 a 5 enteros separados por comas.");
        }
        int[] quantums = new int[partes.length];
        for (int i = 0; i < partes.length; i++) {
            try {
                quantums[i] = Integer.parseInt(partes[i].trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Quantum de nivel inválido: \"" + partes[i].trim() + "\". Debe ser un entero.");
            }
            if (quantums[i] < 1) {
                throw new IllegalArgumentException("El quantum de cada nivel debe ser un entero mayor o igual a 1.");
            }
        }
        return quantums;
    }

    /**
     * Confirma y obtiene los segundos de espera para subir de nivel en MLFQ.
     *
     * @return límite de espera.
     */
    public int getLimiteEsperaSeleccionado() {
        try {
            selectorLimiteEspera.commitEdit();
        } catch (ParseException ex) {
            throw new IllegalArgumentException("El límite de espera debe ser un entero mayor o igual a 1.");
        }
        return (Integer) selectorLimiteEspera.getValue();
    }

    /**
     * Confirma y obtiene el aumento de prioridad por segundo de los nuevos en SRR.
     *
     * @return tasa A.
     */
    public int getTasaASeleccionada() {
        try {
            selectorTasaA.commitEdit();
        } catch (ParseException ex) {
            throw new IllegalArgumentException("La tasa A debe ser un entero mayor o igual a 1.");
        }
        return (Integer) selectorTasaA.getValue();
    }

    /**
     * Confirma y obtiene el aumento de prioridad por segundo de los aceptados en SRR.
     *
     * @return tasa B.
     */
    public int getTasaBSeleccionada() {
        try {
            selectorTasaB.commitEdit();
        } catch (ParseException ex) {
            throw new IllegalArgumentException("La tasa B debe ser un entero mayor o igual a 0.");
        }
        return (Integer) selectorTasaB.getValue();
    }

    /**
     * Obtiene el tipo de asignación de la zona de usuario elegido.
     *
     * @return nombre del tipo de asignación.
     */
    public String getTipoAsignacionSeleccionado() {
        return (String) selectorTipoAsignacion.getSelectedItem();
    }

    /**
     * Confirma y obtiene la cantidad de CPU.
     *
     * @return cantidad de CPU, de 1 a 5.
     */
    public int getCantidadCPUSeleccionada() {
        try {
            selectorCantidadCPU.commitEdit();
        } catch (ParseException ex) {
            throw new IllegalArgumentException("La cantidad de CPU debe ser un entero de 1 a 5.");
        }
        return (Integer) selectorCantidadCPU.getValue();
    }

    /**
     * Confirma y obtiene la cantidad de particiones fijas iguales.
     *
     * @return cantidad de particiones.
     */
    public int getCantidadParticionesSeleccionada() {
        try {
            selectorCantidadParticiones.commitEdit();
        } catch (ParseException ex) {
            throw new IllegalArgumentException("La cantidad de particiones debe ser un entero mayor o igual a 2.");
        }
        return (Integer) selectorCantidadParticiones.getValue();
    }

    /**
     * Obtiene el tamaño de cada partición fija desigual.
     *
     * @return tamaños de partición.
     */
    public int[] getTamañosParticionSeleccionados() {
        String[] partes = selectorTamañosParticion.getText().split(",");
        int[] tamaños = new int[partes.length];
        for (int i = 0; i < partes.length; i++) {
            try {
                tamaños[i] = Integer.parseInt(partes[i].trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Tamaño de partición inválido: \"" + partes[i].trim() + "\". Debe ser un entero.");
            }
        }
        return tamaños;
    }

    // ================================================================
    // SELECCIÓN EN TABLAS
    // ================================================================

    /**
     * Resalta la próxima instrucción en RAM.
     *
     * @param posicionMemoria posición de la instrucción en memoria.
     */
    public void seleccionarProximaInstruccion(int posicionMemoria) {
        seleccionarFila(tablaMemoria, posicionMemoria);
    }

    /**
     * Elimina la selección de ambas tablas.
     */
    public void limpiarSeleccionTablas() {
        tablaMemoria.clearSelection();
        tablaDisco.clearSelection();
    }

    /**
     * Selecciona una fila y la desplaza al área visible.
     *
     * @param tabla tabla que se desea actualizar.
     * @param fila índice de la fila que se desea seleccionar.
     */
    private void seleccionarFila(JTable tabla, int fila) {
        if (fila < 0 || fila >= tabla.getRowCount()) {
            tabla.clearSelection();
            return;
        }
        tabla.setRowSelectionInterval(fila, fila);
        Rectangle areaFila = tabla.getCellRect(fila, 0, true);
        tabla.scrollRectToVisible(areaFila);
    }

    // ================================================================
    // DISTRIBUCIÓN
    // ================================================================

    /**
     * Actualiza el texto de la distribución de memoria.
     */
    private void actualizarDistribucionMemoria() {
        int total = (Integer) selectorMemoria.getValue();
        int kernel = total / 4;
        int usuario = total - kernel;
        lblDistribucionMemoria.setText("<html>"
                + EstiloUI.cuadroColor(EstiloUI.PRIMARIO) + " SO: <b>" + kernel + "</b> &nbsp;&nbsp;&nbsp;"
                + EstiloUI.cuadroColor(EstiloUI.COLOR_USUARIO) + " Usuario: <b>" + usuario + "</b></html>");
        barraMemoria.setValores(new int[]{kernel, usuario},
                new Color[]{EstiloUI.PRIMARIO, EstiloUI.COLOR_USUARIO});
    }

    /**
     * Ajusta el máximo de la memoria virtual.
     */
    private void actualizarLimiteMemoriaVirtual() {
        int tamañoDisco = (Integer) selectorDisco.getValue();
        int maximo = tamañoDisco - tamañoDisco / 20;
        SpinnerNumberModel modelo = (SpinnerNumberModel) selectorMemoriaVirtual.getModel();
        modelo.setMaximum(maximo);
        if ((Integer) selectorMemoriaVirtual.getValue() > maximo) {
            selectorMemoriaVirtual.setValue(maximo);
        }
    }

    /**
     * Actualiza la distribución elegida del disco.
     */
    private void actualizarDistribucionDisco() {
        int tamañoDisco = (Integer) selectorDisco.getValue();
        int tamañoIndices = tamañoDisco / 20;
        int tamañoMemoriaVirtual = (Integer) selectorMemoriaVirtual.getValue();
        int tamañoDatos = tamañoDisco - tamañoIndices - tamañoMemoriaVirtual;
        int maximoVirtual = tamañoDisco - tamañoIndices;
        Color colorIndice = EstiloUI.PRIMARIO;
        Color colorDatos = EstiloUI.COLOR_USUARIO;
        Color colorVirtual = EstiloUI.ACENTO_VIOLETA;
        lblDistribucionDisco.setText("<html>"
                + EstiloUI.cuadroColor(colorIndice) + " Índices: <b>" + tamañoIndices + "</b> &nbsp;&nbsp;"
                + EstiloUI.cuadroColor(colorDatos) + " Programas y datos: <b>" + tamañoDatos + "</b><br>"
                + EstiloUI.cuadroColor(colorVirtual) + " Virtual: <b>" + tamañoMemoriaVirtual + "</b>"
                + " <font color='#7a8594'>(máximo " + maximoVirtual + ")</font></html>");
        barraDisco.setValores(new int[]{tamañoIndices, Math.max(0, tamañoDatos), tamañoMemoriaVirtual},
                new Color[]{colorIndice, colorDatos, colorVirtual});
    }

    // ================================================================
    // ARCHIVOS
    // ================================================================

    /**
     * Selecciona archivos ensamblador.
     */
    private void abrirSelectorArchivos() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos ASM (*.asm)", "asm"));
        fileChooser.setMultiSelectionEnabled(true);
        int resultado = fileChooser.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File[] archivos = fileChooser.getSelectedFiles();
            firePropertyChange("Archivos cargados", null, Arrays.asList(archivos));
        }
    }

    // ================================================================
    // ACCESO PARA EL CONTROLADOR
    // ================================================================

    /**
     * Obtiene el botón de ejecución automática.
     *
     * @return el botón Ejecutar.
     */
    public JButton getBtnEjecutar() {
        return btnEjecutar;
    }

    /**
     * Obtiene el botón de ejecución manual.
     *
     * @return el botón Paso a paso.
     */
    public JButton getBtnPasoAPaso() {
        return btnPasoAPaso;
    }

    /**
     * Obtiene el botón de limpieza.
     *
     * @return el botón Limpiar.
     */
    public JButton getBtnLimpiar() {
        return btnLimpiar;
    }

    /**
     * Obtiene el botón de nueva corrida.
     *
     * @return el botón Nueva corrida.
     */
    public JButton getBtnNuevaCorrida() {
        return btnNuevaCorrida;
    }

    /**
     * Obtiene el botón de carga de archivos.
     *
     * @return el botón Cargar archivos.
     */
    public JButton getBtnCargarArchivo() {
        return btnCargarArchivo;
    }

    /**
     * Obtiene el modelo de la tabla de disco.
     *
     * @return el modelo del disco.
     */
    public DefaultTableModel getModeloDisco() {
        return modeloDisco;
    }

    /**
     * Obtiene el modelo de trabajos pendientes.
     *
     * @return modelo de trabajos y estados.
     */
    public DefaultTableModel getModeloTrabajos() {
        return modeloTrabajos;
    }

    /**
     * Habilita la edición de llegada y prioridad en la lista de trabajos.
     *
     * @param editables true antes del primer paso.
     */
    public void setTrabajosEditables(boolean editables) {
        trabajosEditables = editables;
        if (!editables && tablaTrabajos.isEditing()) {
            tablaTrabajos.getCellEditor().cancelCellEditing();
        }
        tablaTrabajos.repaint();
    }

    /**
     * Muestra el reloj simulado.
     *
     * @param texto reloj y ocio de la CPU.
     */
    public void mostrarReloj(String texto) {
        String limpio = "";
        if (texto != null) {
            limpio = texto.trim();
        }
        lblReloj.setText(limpio.replaceAll("\\s{3,}", "   ·   "));
        lblReloj.setVisible(!limpio.isEmpty());
    }

    /**
     * Obtiene el modelo de procesos cargados en RAM.
     *
     * @return modelo de procesos y estados.
     */
    public DefaultTableModel getModeloProcesos() {
        return modeloProcesos;
    }

    /**
     * Obtiene el modelo de estadísticas.
     *
     * @return modelo de tiempos por proceso.
     */
    public DefaultTableModel getModeloEstadisticas() {
        return modeloEstadisticas;
    }

    /**
     * Obtiene el modelo del historial de corridas.
     *
     * @return modelo con una fila por corrida terminada.
     */
    public DefaultTableModel getModeloHistorial() {
        return modeloHistorial;
    }

    /**
     * Muestra el ocio y los promedios de la corrida actual.
     *
     * @param texto resumen de la corrida.
     */
    public void mostrarResumenCorrida(String texto) {
        lblResumenCorrida.setText(texto);
    }

    /**
     * Obtiene el modelo de la tabla de memoria.
     *
     * @return el modelo de memoria.
     */
    public DefaultTableModel getModeloMemoria() { return modeloMemoria; }

    /**
     * Muestra el resumen de la asignación de la zona de usuario.
     *
     * @param texto resumen de particiones o huecos.
     */
    public void mostrarResumenMemoria(String texto) {
        lblResumenMemoria.setText(texto);
    }

    // ================================================================
    // RENDERERS
    // ================================================================

    /**
     * Colorea la RAM: el primer 25% es el kernel (verde si está libre,
     * ámbar si guarda datos de un BCP) y el resto es el área de usuario.
     */
    private static class RendererMemoria extends EstiloUI.RendererCelda {

        @Override
        protected Color fondo(JTable tabla, Object valor, int fila, int columna) {
            int kernel = tabla.getRowCount() / 4;
            if (fila < kernel) {
                boolean vacia = tabla.getValueAt(fila, 1) == null || tabla.getValueAt(fila, 1).toString().isBlank();
                if (vacia) {
                    return EstiloUI.COLOR_KERNEL;
                }
                return EstiloUI.COLOR_KERNEL_BCP;
            }
            if (esFragmentacion(tabla, fila)) {
                return EstiloUI.COLOR_FRAGMENTACION;
            }
            return super.fondo(tabla, valor, fila, columna);
        }

        /**
         * Indica si una celda está libre dentro de una partición cuyo inicio tiene programa.
         *
         * @param tabla tabla de la RAM.
         * @param fila posición de la RAM.
         * @return true si es fragmentación interna.
         */
        private boolean esFragmentacion(JTable tabla, int fila) {
            Object particion = tabla.getValueAt(fila, 2);
            if (particion == null || particion.toString().isEmpty() || !estaVacia(tabla, fila)) {
                return false;
            }
            int inicio = fila;
            while (inicio > 0 && particion.equals(tabla.getValueAt(inicio - 1, 2))) {
                inicio--;
            }
            return !estaVacia(tabla, inicio);
        }

        /**
         * Indica si una posición de la RAM no tiene contenido.
         *
         * @param tabla tabla de la RAM.
         * @param fila posición de la RAM.
         * @return true si está vacía.
         */
        private boolean estaVacia(JTable tabla, int fila) {
            boolean vacia = tabla.getValueAt(fila, 1) == null || tabla.getValueAt(fila, 1).toString().isBlank();
            return vacia;
        }

        @Override
        protected Color texto(JTable tabla, Object valor, int fila, int columna) {
            if (columna == 0) {
                return EstiloUI.TEXTO_SECUNDARIO;
            }
            return EstiloUI.TEXTO;
        }
    }

    /**
     * Colorea el disco: índice (primer 5%), programas y datos, y memoria virtual (al final).
     */
    private class RendererDisco extends EstiloUI.RendererCelda {

        @Override
        protected Color fondo(JTable tabla, Object valor, int fila, int columna) {
            int total = tabla.getRowCount();
            if (fila < total / 20) {
                return EstiloUI.COLOR_DISCO_INDICE;
            }
            int virtual = 0;
            if (selectorMemoriaVirtual != null) {
                virtual = (Integer) selectorMemoriaVirtual.getValue();
            }
            if (fila >= total - virtual) {
                return EstiloUI.COLOR_DISCO_VIRTUAL;
            }
            return super.fondo(tabla, valor, fila, columna);
        }

        @Override
        protected Color texto(JTable tabla, Object valor, int fila, int columna) {
            if (columna == 0) {
                return EstiloUI.TEXTO_SECUNDARIO;
            }
            String texto = "";
            if (valor != null) {
                texto = valor.toString();
            }
            if (texto.endsWith("(libre)") || texto.endsWith("(sin uso)")) {
                return EstiloUI.TEXTO_SECUNDARIO;
            }
            return EstiloUI.TEXTO;
        }
    }

    /**
     * Listas de trabajos y procesos: el estado se pinta con su color y
     * las celdas editables se resaltan.
     */
    private static class RendererLista extends EstiloUI.RendererCelda {

        private boolean esColumnaEstado(JTable tabla, int columna) {
            return "Estado".equals(tabla.getColumnName(columna));
        }

        @Override
        protected Color fondo(JTable tabla, Object valor, int fila, int columna) {
            if (tabla.isCellEditable(fila, columna)) {
                return EstiloUI.COLOR_EDITABLE;
            }
            return super.fondo(tabla, valor, fila, columna);
        }

        @Override
        protected Color texto(JTable tabla, Object valor, int fila, int columna) {
            if (esColumnaEstado(tabla, columna)) {
                if (valor == null) {
                    return EstiloUI.TEXTO_SECUNDARIO;
                }
                return EstiloUI.colorEstado(valor.toString());
            }
            return EstiloUI.TEXTO;
        }

        @Override
        protected Font fuente(JTable tabla, Object valor, int fila, int columna) {
            if (esColumnaEstado(tabla, columna)) {
                return EstiloUI.FUENTE_MONO_BOLD;
            }
            return tabla.getFont();
        }

        @Override
        protected String tooltip(JTable tabla, Object valor, int fila, int columna) {
            if (tabla.isCellEditable(fila, columna)) {
                return "Doble clic para editar";
            }
            return super.tooltip(tabla, valor, fila, columna);
        }
    }

    /**
     * Panel que se ajusta al ancho del JScrollPane, para que las filas
     * del BCP nunca necesiten desplazamiento horizontal.
     */
    private static class PanelDesplazable extends JPanel implements Scrollable {

        PanelDesplazable(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientacion, int direccion) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientacion, int direccion) {
            int incremento = Math.max(16, visible.height - 32);
            return incremento;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            boolean ajustar = getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
            return ajustar;
        }
    }

    // ================================================================
    // INICIO
    // ================================================================

    /**
     * Inicia la aplicación.
     *
     * @param args argumentos de inicio.
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }
        EstiloUI.instalarTemaGlobal();

        EventQueue.invokeLater(() -> {
            Aplicacion aplicacion = new Aplicacion();
            new Controlador(aplicacion);
            aplicacion.setVisible(true);
        });
    }
}
