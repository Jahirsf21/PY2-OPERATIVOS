package com.mycompany.t1operativos.vista;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Estilos compartidos por las ventanas de la Mini PC.
 *
 * Centraliza colores, tipografías y componentes visuales para mantener
 * una interfaz minimalista y consistente.
 */
public final class EstiloUI {

    private EstiloUI() {
        // Clase de utilidades, no se instancia.
    }

    // ================================================================
    // COLORES
    // ================================================================

    /** Fondo general de las ventanas. */
    public static final Color FONDO = new Color(0xF4, 0xF6, 0xFA);
    /** Fondo de tarjetas y paneles principales. */
    public static final Color PANEL = Color.WHITE;
    /** Bordes suaves. */
    public static final Color BORDE = new Color(0xE3, 0xE7, 0xEE);
    /** Azul principal. */
    public static final Color PRIMARIO = new Color(0x2F, 0x6F, 0xED);
    /** Azul para hover. */
    public static final Color PRIMARIO_HOVER = new Color(0x25, 0x5D, 0xC9);
    /** Azul claro para fondos informativos. */
    public static final Color PRIMARIO_SUAVE = new Color(0xEE, 0xF4, 0xFF);
    /** Rojo para acciones destructivas. */
    public static final Color PELIGRO = new Color(0xE0, 0x50, 0x50);
    /** Rojo para hover. */
    public static final Color PELIGRO_HOVER = new Color(0xC7, 0x3C, 0x3C);
    /** Texto principal. */
    public static final Color TEXTO = new Color(0x22, 0x2B, 0x38);
    /** Texto secundario. */
    public static final Color TEXTO_SECUNDARIO = new Color(0x7A, 0x85, 0x94);

    /** Colores utilizados para estados de procesos. */
    public static final Color ESTADO_NUEVO = PRIMARIO;
    public static final Color ESTADO_LISTO = new Color(0x5B, 0x6B, 0x82);
    public static final Color ESTADO_EJECUTANDO = new Color(0xE0, 0x8E, 0x1D);
    public static final Color ESTADO_TERMINADO = new Color(0x21, 0x96, 0x53);

    /** Acentos de las tarjetas. */
    public static final Color ACENTO_VIOLETA = new Color(0x8E, 0x5C, 0xE0);
    public static final Color ACENTO_AMBAR = ESTADO_EJECUTANDO;
    public static final Color ACENTO_VERDE = ESTADO_TERMINADO;

    /** Zonas de la memoria principal. */
    public static final Color COLOR_KERNEL = new Color(0xE8, 0xF5, 0xE9);
    public static final Color COLOR_KERNEL_BCP = new Color(0xFF, 0xF3, 0xDC);
    public static final Color COLOR_USUARIO = new Color(0xD8, 0xE4, 0xFF);

    /** Zonas del disco. */
    public static final Color COLOR_DISCO_INDICE = new Color(0xE3, 0xF2, 0xFD);
    public static final Color COLOR_DISCO_VIRTUAL = new Color(0xF1, 0xE8, 0xFB);

    /** Celdas que el usuario puede editar. */
    public static final Color COLOR_EDITABLE = new Color(0xFF, 0xFB, 0xEA);

    /** Consola. */
    public static final Color CONSOLA_FONDO = new Color(0x1B, 0x22, 0x30);
    public static final Color CONSOLA_TEXTO = new Color(0xD6, 0xE2, 0xF5);
    public static final Color CONSOLA_PULGAR = new Color(0x3A, 0x46, 0x5A);
    public static final Color CONSOLA_PULGAR_HOVER = new Color(0x55, 0x63, 0x7A);

    /** Barras de desplazamiento. */
    public static final Color PULGAR = new Color(0xC9, 0xD0, 0xDB);
    public static final Color PULGAR_HOVER = new Color(0x9A, 0xA5, 0xB5);

    /** Ancho máximo para que un componente se estire dentro de un BoxLayout. */
    public static final int ANCHO_LIBRE = 10000;

    // ================================================================
    // TIPOGRAFÍAS
    // ================================================================

    public static final Font FUENTE_BASE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FUENTE_TITULO = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FUENTE_ETIQUETA = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FUENTE_ETIQUETA_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FUENTE_MONO = new Font("Consolas", Font.PLAIN, 13);
    public static final Font FUENTE_MONO_BOLD = new Font("Consolas", Font.BOLD, 13);
    public static final Font FUENTE_TITULO_VENTANA = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FUENTE_NUMERO = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FUENTE_SECCION = new Font("Segoe UI", Font.BOLD, 11);

    // ================================================================
    // TEMA GLOBAL
    // ================================================================

    /**
     * Aplica valores por defecto de Swing para que los componentes
     * creados por el sistema (diálogos, selector de archivos, tooltips)
     * combinen con el diseño de la Mini PC.
     *
     * Se llama después de elegir el Look and Feel.
     */
    public static void instalarTemaGlobal() {
        UIManager.put("ScrollBarUI", BarraDesplazamientoUI.class.getName());
        UIManager.put("ToolTip.background", PANEL);
        UIManager.put("ToolTip.foreground", TEXTO);
        UIManager.put("ToolTip.font", FUENTE_ETIQUETA);
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        UIManager.put("OptionPane.background", FONDO);
        UIManager.put("Panel.background", FONDO);
        UIManager.put("OptionPane.messageFont", FUENTE_BASE);
        UIManager.put("OptionPane.buttonFont", FUENTE_BASE);
        UIManager.put("OptionPane.messageForeground", TEXTO);
    }

    // ================================================================
    // BOTONES
    // ================================================================

    /** Crea un botón principal con fondo sólido. */
    public static JButton crearBotonPrimario(String texto, Color color, Color colorHover) {
        return new BotonPlano(texto, color, colorHover, Color.WHITE, null);
    }

    /** Crea un botón secundario blanco con borde. */
    public static JButton crearBotonSecundario(String texto) {
        return new BotonPlano(texto, PANEL, FONDO, TEXTO, BORDE);
    }

    /**
     * Botón personalizado para mantener el mismo aspecto
     * independientemente del Look and Feel de Windows.
     */
    private static class BotonPlano extends JButton {

        private final Color colorBase;
        private final Color colorHover;
        private final Color colorBorde;
        private boolean sobreBoton = false;

        BotonPlano(String texto, Color colorBase, Color colorHover, Color colorTexto, Color colorBorde) {
            super(texto);
            this.colorBase = colorBase;
            this.colorHover = colorHover;
            this.colorBorde = colorBorde;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(colorTexto);
            setFont(FUENTE_TITULO);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    sobreBoton = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    sobreBoton = false;
                    repaint();
                }
            });
        }

        @Override
        public void setEnabled(boolean habilitado) {
            super.setEnabled(habilitado);
            if (habilitado) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            } else {
                setCursor(Cursor.getDefaultCursor());
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color relleno;
            if (!isEnabled()) {
                relleno = new Color(0xE8, 0xE9, 0xED);
            } else {
                if (sobreBoton) {
                    relleno = colorHover;
                } else {
                    relleno = colorBase;
                }
            }
            g2.setColor(relleno);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            if (colorBorde != null) {
                if (isEnabled()) {
                    g2.setColor(colorBorde);
                } else {
                    g2.setColor(new Color(0xD8, 0xDA, 0xE0));
                }
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ================================================================
    // ETIQUETAS
    // ================================================================

    /** Título normal de tarjeta o sección. */
    public static JLabel crearTitulo(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(FUENTE_TITULO);
        etiqueta.setForeground(TEXTO);
        return etiqueta;
    }

    /** Título principal de una ventana. */
    public static JLabel crearTituloVentana(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(FUENTE_TITULO_VENTANA);
        etiqueta.setForeground(TEXTO);
        return etiqueta;
    }

    /** Texto secundario. */
    public static JLabel crearEtiquetaSecundaria(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(FUENTE_ETIQUETA);
        etiqueta.setForeground(TEXTO_SECUNDARIO);
        return etiqueta;
    }

    /** Etiqueta pequeña utilizada para identificar una sección. */
    public static JLabel crearEtiquetaSeccion(String texto) {
        JLabel etiqueta = new JLabel(texto.toUpperCase());
        etiqueta.setFont(FUENTE_SECCION);
        etiqueta.setForeground(TEXTO_SECUNDARIO);
        return etiqueta;
    }

    /** Crea una etiqueta en forma de píldora (fondo redondeado). */
    public static Pildora crearPildora(String texto, Color fondo, Color frente) {
        return new Pildora(texto, fondo, frente);
    }

    /**
     * Etiqueta con fondo redondeado, usada para el reloj y los estados.
     */
    public static class Pildora extends JLabel {

        private Color fondoPildora;

        Pildora(String texto, Color fondo, Color frente) {
            super(texto);
            this.fondoPildora = fondo;
            setOpaque(false);
            setForeground(frente);
            setFont(FUENTE_ETIQUETA_BOLD);
            setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        }

        /** Cambia los colores de la píldora. */
        public void setColores(Color fondo, Color frente) {
            this.fondoPildora = fondo;
            setForeground(frente);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fondoPildora);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ================================================================
    // TARJETAS
    // ================================================================

    /** Crea una tarjeta blanca con borde suave. */
    public static JPanel crearTarjeta() {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        return panel;
    }

    /**
     * Crea una tarjeta blanca con una franja de color en la parte superior.
     *
     * @param acento color de la franja.
     * @return tarjeta con BorderLayout.
     */
    public static JPanel crearTarjeta(Color acento) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL);
        Border exterior = BorderFactory.createLineBorder(BORDE);
        Border franja = BorderFactory.createMatteBorder(3, 0, 0, 0, acento);
        Border interior = BorderFactory.createEmptyBorder(10, 14, 12, 14);
        panel.setBorder(BorderFactory.createCompoundBorder(exterior,
                BorderFactory.createCompoundBorder(franja, interior)));
        return panel;
    }

    /**
     * Envuelve un componente en una tarjeta con título e ícono.
     *
     * @param titulo texto del título.
     * @param icono ícono del título, o null.
     * @param acento color de la franja superior.
     * @param contenido componente central.
     * @return la tarjeta.
     */
    public static JPanel crearTarjetaConTitulo(String titulo, Icon icono, Color acento, Component contenido) {
        JPanel tarjeta = crearTarjeta(acento);
        JLabel etiqueta = crearTitulo(titulo);
        etiqueta.setIcon(icono);
        etiqueta.setIconTextGap(8);
        etiqueta.setBorder(BorderFactory.createEmptyBorder(0, 2, 8, 2));
        tarjeta.add(etiqueta, BorderLayout.NORTH);
        tarjeta.add(contenido, BorderLayout.CENTER);
        return tarjeta;
    }

    /** Separador horizontal de un píxel. */
    public static JPanel crearSeparador() {
        JPanel linea = new JPanel();
        linea.setBackground(BORDE);
        linea.setMaximumSize(new Dimension(ANCHO_LIBRE, 1));
        linea.setPreferredSize(new Dimension(10, 1));
        linea.setMinimumSize(new Dimension(1, 1));
        return linea;
    }

    /** Cuadrito de color para leyendas en HTML. */
    public static String cuadroColor(Color color) {
        return String.format("<span style='background-color:#%02x%02x%02x;'>&nbsp;&nbsp;&nbsp;&nbsp;</span>",
                color.getRed(), color.getGreen(), color.getBlue());
    }

    /** Mezcla un color con blanco; útil para fondos suaves. */
    public static Color suavizar(Color color, double proporcionBlanco) {
        double p = Math.max(0, Math.min(1, proporcionBlanco));
        int r = (int) Math.round(color.getRed() + (255 - color.getRed()) * p);
        int g = (int) Math.round(color.getGreen() + (255 - color.getGreen()) * p);
        int b = (int) Math.round(color.getBlue() + (255 - color.getBlue()) * p);
        return new Color(r, g, b);
    }

    /** Color asociado a un estado de proceso. */
    public static Color colorEstado(String estado) {
        if (estado == null) {
            return TEXTO_SECUNDARIO;
        }
        switch (estado.trim().toUpperCase()) {
            case "EJECUTANDO":
                return ESTADO_EJECUTANDO;
            case "TERMINADO":
                return ESTADO_TERMINADO;
            case "LISTO":
                return ESTADO_LISTO;
            case "NUEVO":
                return ESTADO_NUEVO;
            default:
                return TEXTO_SECUNDARIO;
        }
    }

    // ================================================================
    // CAMPOS
    // ================================================================

    /** Aplica el estilo de la Mini PC a un JSpinner sin reemplazar su editor. */
    public static void estilizarSpinner(JSpinner spinner) {
        spinner.setFont(FUENTE_MONO);
        spinner.setPreferredSize(new Dimension(130, 34));
        spinner.setMinimumSize(new Dimension(100, 34));
        spinner.setBorder(BorderFactory.createLineBorder(BORDE));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField campo = ((JSpinner.DefaultEditor) editor).getTextField();
            campo.setFont(FUENTE_MONO);
            campo.setForeground(TEXTO);
            campo.setBackground(PANEL);
            campo.setHorizontalAlignment(SwingConstants.LEFT);
            campo.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        }
    }

    /** Aplica el estilo de la Mini PC a un JComboBox. */
    public static <E> void estilizarCombo(JComboBox<E> combo) {
        combo.setFont(FUENTE_BASE);
        combo.setForeground(TEXTO);
        combo.setBackground(PANEL);
        combo.setPreferredSize(new Dimension(160, 34));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                    boolean seleccionado, boolean foco) {
                super.getListCellRendererComponent(lista, valor, indice, seleccionado, false);
                setFont(FUENTE_BASE);
                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                if (seleccionado && indice >= 0) {
                    setBackground(PRIMARIO);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(PANEL);
                    setForeground(TEXTO);
                }
                return this;
            }
        });
    }

    // ================================================================
    // TABLAS
    // ================================================================

    /**
     * Renderer base de las tablas: filas alternadas, relleno lateral y
     * selección en azul. Las subclases pueden cambiar fondo, texto y fuente.
     */
    public static class RendererCelda extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionado,
                boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, seleccionado, false, fila, columna);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            setFont(fuente(tabla, valor, fila, columna));
            if (seleccionado) {
                setBackground(PRIMARIO);
                setForeground(Color.WHITE);
            } else {
                setBackground(fondo(tabla, valor, fila, columna));
                setForeground(texto(tabla, valor, fila, columna));
            }
            setToolTipText(tooltip(tabla, valor, fila, columna));
            return this;
        }

        /** Fondo de una celda no seleccionada. */
        protected Color fondo(JTable tabla, Object valor, int fila, int columna) {
            if (fila % 2 == 0) {
                return PANEL;
            }
            return FONDO;
        }

        /** Color del texto de una celda no seleccionada. */
        protected Color texto(JTable tabla, Object valor, int fila, int columna) {
            return TEXTO;
        }

        /** Fuente de la celda. */
        protected Font fuente(JTable tabla, Object valor, int fila, int columna) {
            return tabla.getFont();
        }

        /** Tooltip de la celda: su contenido completo. */
        protected String tooltip(JTable tabla, Object valor, int fila, int columna) {
            if (valor == null || valor.toString().isBlank()) {
                return null;
            }
            return valor.toString();
        }
    }

    /** Aplica el estilo general de la Mini PC a una tabla con el renderer base. */
    public static void aplicarEstiloTabla(JTable tabla) {
        aplicarEstiloTabla(tabla, new RendererCelda());
    }

    /** Aplica el estilo general de la Mini PC a una tabla con un renderer propio. */
    public static void aplicarEstiloTabla(JTable tabla, RendererCelda renderer) {
        tabla.setRowHeight(26);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setFont(FUENTE_MONO);
        tabla.setForeground(TEXTO);
        tabla.setBackground(PANEL);
        tabla.setSelectionBackground(PRIMARIO);
        tabla.setSelectionForeground(Color.WHITE);
        tabla.setFillsViewportHeight(true);

        JTableHeader encabezado = tabla.getTableHeader();
        encabezado.setBackground(PANEL);
        encabezado.setReorderingAllowed(false);
        encabezado.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionado,
                    boolean foco, int fila, int columna) {
                super.getTableCellRendererComponent(t, valor, false, false, fila, columna);
                setFont(FUENTE_ETIQUETA_BOLD);
                setForeground(TEXTO_SECUNDARIO);
                setBackground(PANEL);
                setOpaque(true);
                setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDE),
                        BorderFactory.createEmptyBorder(7, 10, 7, 10)));
                return this;
            }
        });

        tabla.setDefaultRenderer(Object.class, renderer);
        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    // ================================================================
    // DESPLAZAMIENTO
    // ================================================================

    /** Crea un JScrollPane claro con barras delgadas. */
    public static JScrollPane crearScroll(Component contenido) {
        JScrollPane scroll = new JScrollPane(contenido);
        scroll.setBorder(BorderFactory.createLineBorder(BORDE));
        scroll.getViewport().setBackground(PANEL);
        aplicarBarras(scroll, PULGAR, PULGAR_HOVER, PANEL);
        return scroll;
    }

    /**
     * Instala barras delgadas, redondeadas y sin flechas en un JScrollPane.
     *
     * @param scroll panel de desplazamiento.
     * @param pulgar color del pulgar.
     * @param pulgarHover color del pulgar con el mouse encima.
     * @param pista color de fondo de la barra.
     */
    public static void aplicarBarras(JScrollPane scroll, Color pulgar, Color pulgarHover, Color pista) {
        JScrollBar vertical = scroll.getVerticalScrollBar();
        JScrollBar horizontal = scroll.getHorizontalScrollBar();
        vertical.setUI(new BarraDesplazamientoUI(pulgar, pulgarHover, pista));
        horizontal.setUI(new BarraDesplazamientoUI(pulgar, pulgarHover, pista));
        vertical.setUnitIncrement(16);
        horizontal.setUnitIncrement(16);
        for (String esquina : new String[]{ScrollPaneConstants.UPPER_RIGHT_CORNER,
            ScrollPaneConstants.LOWER_RIGHT_CORNER, ScrollPaneConstants.LOWER_LEFT_CORNER}) {
            JPanel relleno = new JPanel();
            relleno.setBackground(pista);
            scroll.setCorner(esquina, relleno);
        }
    }

    /**
     * Barra de desplazamiento minimalista: delgada, con pulgar redondeado
     * y sin botones de flecha.
     */
    public static class BarraDesplazamientoUI extends BasicScrollBarUI {

        private static final int GROSOR = 10;
        private static final int MARGEN = 2;

        private final Color colorPulgar;
        private final Color colorPulgarHover;
        private final Color colorPista;

        /** Barra con los colores claros por defecto. */
        public BarraDesplazamientoUI() {
            this(PULGAR, PULGAR_HOVER, PANEL);
        }

        /** Barra con colores propios. */
        public BarraDesplazamientoUI(Color pulgar, Color pulgarHover, Color pista) {
            this.colorPulgar = pulgar;
            this.colorPulgarHover = pulgarHover;
            this.colorPista = pista;
        }

        /** Requerido por UIManager para instalarla de forma global. */
        public static ComponentUI createUI(JComponent componente) {
            return new BarraDesplazamientoUI();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            scrollbar.setOpaque(true);
            scrollbar.setBackground(colorPista);
        }

        @Override
        protected JButton createDecreaseButton(int orientacion) {
            return crearBotonInvisible();
        }

        @Override
        protected JButton createIncreaseButton(int orientacion) {
            return crearBotonInvisible();
        }

        private JButton crearBotonInvisible() {
            JButton boton = new JButton();
            Dimension cero = new Dimension(0, 0);
            boton.setPreferredSize(cero);
            boton.setMinimumSize(cero);
            boton.setMaximumSize(cero);
            boton.setFocusable(false);
            boton.setBorder(BorderFactory.createEmptyBorder());
            boton.setOpaque(false);
            boton.setContentAreaFilled(false);
            return boton;
        }

        @Override
        public Dimension getPreferredSize(JComponent componente) {
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                return new Dimension(GROSOR, 48);
            }
            return new Dimension(48, GROSOR);
        }

        @Override
        protected Dimension getMinimumThumbSize() {
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                return new Dimension(GROSOR, 32);
            }
            return new Dimension(32, GROSOR);
        }

        @Override
        protected void paintTrack(Graphics g, JComponent componente, Rectangle area) {
            g.setColor(colorPista);
            g.fillRect(area.x, area.y, area.width, area.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent componente, Rectangle area) {
            if (area.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isDragging || isThumbRollover()) {
                g2.setColor(colorPulgarHover);
            } else {
                g2.setColor(colorPulgar);
            }
            int x = area.x + MARGEN;
            int y = area.y + MARGEN;
            int ancho = Math.max(0, area.width - 2 * MARGEN);
            int alto = Math.max(0, area.height - 2 * MARGEN);
            int arco = Math.min(ancho, alto);
            g2.fillRoundRect(x, y, ancho, alto, arco, arco);
            g2.dispose();
        }
    }

    // ================================================================
    // BARRA DE DISTRIBUCIÓN
    // ================================================================

    /**
     * Barra horizontal redondeada dividida en segmentos proporcionales.
     * Se usa para mostrar cómo se reparte la RAM y el disco.
     */
    public static class BarraDistribucion extends JComponent {

        private int[] valores = new int[0];
        private Color[] colores = new Color[0];

        public BarraDistribucion() {
            setPreferredSize(new Dimension(400, 14));
            setMinimumSize(new Dimension(100, 14));
            setMaximumSize(new Dimension(ANCHO_LIBRE, 14));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        /** Actualiza los segmentos representados. */
        public void setValores(int[] valores, Color[] colores) {
            this.valores = valores;
            this.colores = colores;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            int ancho = getWidth();
            int alto = getHeight();
            if (ancho <= 0 || alto <= 0) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D forma = new RoundRectangle2D.Float(0, 0, ancho, alto, alto, alto);
            g2.setColor(BORDE);
            g2.fill(forma);

            int total = 0;
            for (int valor : valores) {
                total += Math.max(0, valor);
            }
            if (total > 0) {
                g2.clip(forma);
                int x = 0;
                for (int i = 0; i < valores.length; i++) {
                    int segmento = ancho - x;
                    if (i < valores.length - 1) {
                        segmento = (int) Math.round(ancho * (Math.max(0, valores[i]) / (double) total));
                    }
                    g2.setColor(colores[i % colores.length]);
                    g2.fillRect(x, 0, segmento, alto);
                    x += segmento;
                }
            }
            g2.dispose();
        }
    }

    // ================================================================
    // ÍCONOS
    // ================================================================

    /** Tamaño de los íconos SVG de los títulos de las tarjetas. */
    private static final int TAMANO_ICONO_TARJETA = 18;

    /** Ícono de la tarjeta RAM (RAM.svg). */
    public static Icon iconoTarjetaRAM() {
        return IconoSVG.cargar("RAM.svg", TAMANO_ICONO_TARJETA, iconoMemoria(PRIMARIO));
    }

    /** Ícono de la tarjeta Disco (Disco.svg). */
    public static Icon iconoTarjetaDisco() {
        return IconoSVG.cargar("Disco.svg", TAMANO_ICONO_TARJETA, iconoDisco(ACENTO_VIOLETA));
    }

    /** Ícono de la tarjeta BCP actual (BCP.svg). */
    public static Icon iconoTarjetaBCP() {
        return IconoSVG.cargar("BCP.svg", TAMANO_ICONO_TARJETA, iconoSistema(PRIMARIO));
    }

    /** Ícono de la tarjeta Lista de Trabajos (ListaTrabajos.svg). */
    public static Icon iconoTarjetaTrabajos() {
        return IconoSVG.cargar("ListaTrabajos.svg", TAMANO_ICONO_TARJETA, iconoLista(ACENTO_AMBAR));
    }

    /** Ícono de la tarjeta Lista de Procesos (ListaProcesos.svg). */
    public static Icon iconoTarjetaProcesos() {
        return IconoSVG.cargar("ListaProcesos.svg", TAMANO_ICONO_TARJETA, iconoUsuario(ACENTO_VERDE));
    }

    /** Ícono de la tarjeta Consola (Consola.svg). */
    public static Icon iconoTarjetaConsola() {
        return IconoSVG.cargar("Consola.svg", TAMANO_ICONO_TARJETA, iconoConsola(TEXTO));
    }

    public static Icon iconoMemoria(Color color) {
        return new IconoSimple(IconoSimple.Tipo.MEMORIA, color, 15);
    }

    public static Icon iconoSistema(Color color) {
        return new IconoSimple(IconoSimple.Tipo.SISTEMA, color, 15);
    }

    public static Icon iconoUsuario(Color color) {
        return new IconoSimple(IconoSimple.Tipo.USUARIO, color, 15);
    }

    public static Icon iconoReloj(Color color) {
        return new IconoSimple(IconoSimple.Tipo.RELOJ, color, 15);
    }

    public static Icon iconoDisco(Color color) {
        return new IconoSimple(IconoSimple.Tipo.DISCO, color, 15);
    }

    public static Icon iconoLista(Color color) {
        return new IconoSimple(IconoSimple.Tipo.LISTA, color, 15);
    }

    public static Icon iconoConsola(Color color) {
        return new IconoSimple(IconoSimple.Tipo.CONSOLA, color, 15);
    }

    /**
     * Íconos vectoriales simples.
     *
     * De esta forma no se necesitan imágenes externas
     * ni emojis dependientes del sistema operativo.
     */
    private static class IconoSimple implements Icon {

        enum Tipo { MEMORIA, SISTEMA, USUARIO, RELOJ, DISCO, LISTA, CONSOLA }

        private final Tipo tipo;
        private final Color color;
        private final int tam;

        IconoSimple(Tipo tipo, Color color, int tam) {
            this.tipo = tipo;
            this.color = color;
            this.tam = tam;
        }

        @Override
        public int getIconWidth() {
            return tam;
        }

        @Override
        public int getIconHeight() {
            return tam;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (tipo) {
                case MEMORIA:
                    dibujarMemoria(g2);
                    break;
                case SISTEMA:
                    dibujarSistema(g2);
                    break;
                case USUARIO:
                    dibujarUsuario(g2);
                    break;
                case RELOJ:
                    dibujarReloj(g2);
                    break;
                case DISCO:
                    dibujarDisco(g2);
                    break;
                case LISTA:
                    dibujarLista(g2);
                    break;
                case CONSOLA:
                    dibujarConsola(g2);
                    break;
                default:
                    break;
            }
            g2.dispose();
        }

        /** Chip de memoria. */
        private void dibujarMemoria(Graphics2D g2) {
            g2.drawRoundRect(3, 3, tam - 6, tam - 6, 2, 2);
            for (int i = 0; i < 3; i++) {
                int pos = 5 + i * 3;
                g2.drawLine(pos, 0, pos, 3);
                g2.drawLine(pos, tam - 3, pos, tam);
            }
        }

        /** Escudo para el Sistema Operativo. */
        private void dibujarSistema(Graphics2D g2) {
            int pad = 2;
            Polygon escudo = new Polygon();
            escudo.addPoint(tam / 2, pad);
            escudo.addPoint(tam - pad, pad + 3);
            escudo.addPoint(tam - pad, tam / 2 + 1);
            escudo.addPoint(tam / 2, tam - pad);
            escudo.addPoint(pad, tam / 2 + 1);
            escudo.addPoint(pad, pad + 3);
            g2.drawPolygon(escudo);
        }

        /** Usuario. */
        private void dibujarUsuario(Graphics2D g2) {
            int diametroCabeza = tam / 2 - 1;
            g2.drawOval((tam - diametroCabeza) / 2, 1, diametroCabeza, diametroCabeza);
            g2.drawArc(1, tam / 2, tam - 2, tam / 2, 0, 180);
        }

        /** Reloj. */
        private void dibujarReloj(Graphics2D g2) {
            g2.drawOval(1, 1, tam - 3, tam - 3);
            int centro = tam / 2;
            g2.drawLine(centro, centro, centro, 4);
            g2.drawLine(centro, centro, centro + 3, centro + 2);
        }

        /** Unidad de disco. */
        private void dibujarDisco(Graphics2D g2) {
            g2.drawRoundRect(1, 3, tam - 3, tam - 6, 4, 4);
            g2.drawLine(1, tam / 2 + 1, tam - 2, tam / 2 + 1);
            g2.fillOval(tam - 6, tam / 2 + 3, 2, 2);
        }

        /** Lista con viñetas. */
        private void dibujarLista(Graphics2D g2) {
            for (int i = 0; i < 3; i++) {
                int y = 3 + i * 4;
                g2.fillOval(1, y - 1, 3, 3);
                g2.drawLine(6, y, tam - 2, y);
            }
        }

        /** Terminal. */
        private void dibujarConsola(Graphics2D g2) {
            g2.drawRoundRect(1, 2, tam - 3, tam - 5, 3, 3);
            g2.drawLine(4, 6, 6, 8);
            g2.drawLine(6, 8, 4, 10);
            g2.drawLine(8, 10, 11, 10);
        }
    }
}
