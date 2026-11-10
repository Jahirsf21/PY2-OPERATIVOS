package com.mycompany.t1operativos.vista;

import javax.swing.Icon;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ícono que dibuja los trazados ({@code <path>}) de un archivo SVG
 * guardado en {@code src/main/resources/iconos/}.
 *
 * Swing no dibuja SVG por sí mismo, así que esta clase interpreta los
 * comandos de trazado (M, L, H, V, C, S, Q, T, Z, en mayúscula o minúscula)
 * y los convierte en figuras de Java2D. La figura se escala para caber
 * en el tamaño pedido, por lo que se ve nítida en cualquier escala de pantalla.
 */
public final class IconoSVG implements Icon {

    private static final Pattern ETIQUETA = Pattern.compile("<(path|g|svg)\\b([^>]*)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATRIBUTO_D = Pattern.compile("\\sd=\"([^\"]*)\"");
    private static final Pattern RELLENO = Pattern.compile("(?:\\sfill=\"|fill:\\s*)(#[0-9a-fA-F]{6}|none)");
    private static final Pattern REGLA = Pattern.compile("fill-rule=\"(evenodd|nonzero)\"");

    private final List<Shape> figuras = new ArrayList<>();
    private final List<Color> colores = new ArrayList<>();
    private final int tamano;
    private Rectangle2D limites;

    private IconoSVG(int tamano) {
        this.tamano = tamano;
    }

    /**
     * Carga un ícono de la carpeta de recursos {@code /iconos}.
     *
     * @param nombre nombre del archivo, por ejemplo "RAM.svg".
     * @param tamano ancho y alto del ícono en píxeles.
     * @param respaldo ícono a usar si el archivo no existe o no se puede leer.
     * @return el ícono SVG, o el respaldo.
     */
    public static Icon cargar(String nombre, int tamano, Icon respaldo) {
        try (InputStream entrada = IconoSVG.class.getResourceAsStream("/iconos/" + nombre)) {
            if (entrada == null) {
                return respaldo;
            }
            String svg = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            IconoSVG icono = new IconoSVG(tamano);
            icono.interpretar(svg);
            if (icono.figuras.isEmpty()) {
                return respaldo;
            }
            return icono;
        } catch (IOException | RuntimeException ex) {
            return respaldo;
        }
    }

    /**
     * Recorre las etiquetas del SVG. El color y la regla de relleno de cada
     * trazado se toman del propio trazado o, si no los declara, del último
     * grupo que los haya declarado antes.
     */
    private void interpretar(String svg) {
        Color colorHeredado = Color.BLACK;
        int reglaHeredada = Path2D.WIND_NON_ZERO;
        Matcher etiquetas = ETIQUETA.matcher(svg);
        while (etiquetas.find()) {
            String atributos = etiquetas.group(2);
            Color colorPropio = leerColor(atributos);
            Integer reglaPropia = leerRegla(atributos);
            if (!"path".equalsIgnoreCase(etiquetas.group(1))) {
                if (colorPropio != null) {
                    colorHeredado = colorPropio;
                }
                if (reglaPropia != null) {
                    reglaHeredada = reglaPropia;
                }
                continue;
            }
            Matcher d = ATRIBUTO_D.matcher(atributos);
            if (!d.find()) {
                continue;
            }
            Path2D.Double figura = interpretarTrazado(d.group(1));
            if (reglaPropia != null) {
                figura.setWindingRule(reglaPropia);
            } else {
                figura.setWindingRule(reglaHeredada);
            }
            figuras.add(figura);
            if (colorPropio != null) {
                colores.add(colorPropio);
            } else {
                colores.add(colorHeredado);
            }
            Rectangle2D caja = figura.getBounds2D();
            if (limites == null) {
                limites = caja;
            } else {
                limites = limites.createUnion(caja);
            }
        }
    }

    /** Lee un color de relleno; devuelve null si no hay uno o es "none". */
    private static Color leerColor(String atributos) {
        Matcher m = RELLENO.matcher(atributos);
        Color color = null;
        while (m.find()) {
            String valor = m.group(1);
            if (!"none".equalsIgnoreCase(valor)) {
                color = Color.decode(valor);
            }
        }
        return color;
    }

    private static Integer leerRegla(String atributos) {
        Matcher m = REGLA.matcher(atributos);
        if (!m.find()) {
            return null;
        }
        if ("evenodd".equals(m.group(1))) {
            return Path2D.WIND_EVEN_ODD;
        }
        return Path2D.WIND_NON_ZERO;
    }

    // ================================================================
    // TRAZADOS
    // ================================================================

    /** Convierte el atributo "d" de un trazado SVG en un Path2D. */
    private static Path2D.Double interpretarTrazado(String datos) {
        Path2D.Double ruta = new Path2D.Double();
        Lector lector = new Lector(datos);
        double x = 0, y = 0;            // punto actual
        double inicioX = 0, inicioY = 0; // inicio del subtrazado
        double controlX = 0, controlY = 0; // último punto de control (para S y T)
        char anterior = ' ';
        char comando = ' ';

        while (lector.hayMas()) {
            if (lector.siguienteEsComando()) {
                comando = lector.leerComando();
            } else if (comando == 'M') {
                comando = 'L';           // coordenadas extra tras M son líneas
            } else if (comando == 'm') {
                comando = 'l';
            } else if (comando == ' ') {
                break;
            }
            boolean relativo = Character.isLowerCase(comando);
            double baseX = 0;
            double baseY = 0;
            if (relativo) {
                baseX = x;
                baseY = y;
            }

            switch (Character.toUpperCase(comando)) {
                case 'M':
                    x = baseX + lector.numero();
                    y = baseY + lector.numero();
                    ruta.moveTo(x, y);
                    inicioX = x;
                    inicioY = y;
                    break;
                case 'L':
                    x = baseX + lector.numero();
                    y = baseY + lector.numero();
                    ruta.lineTo(x, y);
                    break;
                case 'H':
                    x = baseX + lector.numero();
                    ruta.lineTo(x, y);
                    break;
                case 'V':
                    y = baseY + lector.numero();
                    ruta.lineTo(x, y);
                    break;
                case 'C': {
                    double x1 = baseX + lector.numero();
                    double y1 = baseY + lector.numero();
                    double x2 = baseX + lector.numero();
                    double y2 = baseY + lector.numero();
                    x = baseX + lector.numero();
                    y = baseY + lector.numero();
                    ruta.curveTo(x1, y1, x2, y2, x, y);
                    controlX = x2;
                    controlY = y2;
                    break;
                }
                case 'S': {
                    double x1 = x;
                    double y1 = y;
                    if ("CcSs".indexOf(anterior) >= 0) {
                        x1 = 2 * x - controlX;
                        y1 = 2 * y - controlY;
                    }
                    double x2 = baseX + lector.numero();
                    double y2 = baseY + lector.numero();
                    x = baseX + lector.numero();
                    y = baseY + lector.numero();
                    ruta.curveTo(x1, y1, x2, y2, x, y);
                    controlX = x2;
                    controlY = y2;
                    break;
                }
                case 'Q': {
                    double x1 = baseX + lector.numero();
                    double y1 = baseY + lector.numero();
                    x = baseX + lector.numero();
                    y = baseY + lector.numero();
                    ruta.quadTo(x1, y1, x, y);
                    controlX = x1;
                    controlY = y1;
                    break;
                }
                case 'T': {
                    double x1 = x;
                    double y1 = y;
                    if ("QqTt".indexOf(anterior) >= 0) {
                        x1 = 2 * x - controlX;
                        y1 = 2 * y - controlY;
                    }
                    x = baseX + lector.numero();
                    y = baseY + lector.numero();
                    ruta.quadTo(x1, y1, x, y);
                    controlX = x1;
                    controlY = y1;
                    break;
                }
                case 'Z':
                    ruta.closePath();
                    x = inicioX;
                    y = inicioY;
                    break;
                default:
                    // Comando no soportado (por ejemplo arcos): se detiene aquí.
                    return ruta;
            }
            anterior = comando;
        }
        return ruta;
    }

    /** Lee comandos y números del atributo "d". */
    private static final class Lector {

        private final String texto;
        private int pos;

        Lector(String texto) {
            this.texto = texto;
        }

        private void saltarSeparadores() {
            while (pos < texto.length()) {
                char c = texto.charAt(pos);
                if (c == ',' || Character.isWhitespace(c)) {
                    pos++;
                } else {
                    break;
                }
            }
        }

        boolean hayMas() {
            saltarSeparadores();
            return pos < texto.length();
        }

        boolean siguienteEsComando() {
            saltarSeparadores();
            return pos < texto.length() && Character.isLetter(texto.charAt(pos))
                    && texto.charAt(pos) != 'e' && texto.charAt(pos) != 'E';
        }

        char leerComando() {
            saltarSeparadores();
            return texto.charAt(pos++);
        }

        double numero() {
            saltarSeparadores();
            int inicio = pos;
            if (pos < texto.length() && (texto.charAt(pos) == '-' || texto.charAt(pos) == '+')) {
                pos++;
            }
            boolean punto = false;
            while (pos < texto.length()) {
                char c = texto.charAt(pos);
                if (Character.isDigit(c)) {
                    pos++;
                } else if (c == '.' && !punto) {
                    punto = true;
                    pos++;
                } else if ((c == 'e' || c == 'E') && pos + 1 < texto.length()) {
                    pos++;
                    if (texto.charAt(pos) == '-' || texto.charAt(pos) == '+') {
                        pos++;
                    }
                } else {
                    break;
                }
            }
            if (inicio == pos) {
                throw new IllegalArgumentException("Número esperado en la posición " + pos);
            }
            return Double.parseDouble(texto.substring(inicio, pos));
        }
    }

    // ================================================================
    // ICON
    // ================================================================

    @Override
    public int getIconWidth() {
        return tamano;
    }

    @Override
    public int getIconHeight() {
        return tamano;
    }

    @Override
    public void paintIcon(Component componente, Graphics g, int x, int y) {
        if (limites == null || limites.getWidth() <= 0 || limites.getHeight() <= 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        double escala = tamano / Math.max(limites.getWidth(), limites.getHeight());
        double desplazamientoX = x + (tamano - limites.getWidth() * escala) / 2.0;
        double desplazamientoY = y + (tamano - limites.getHeight() * escala) / 2.0;
        AffineTransform transformacion = new AffineTransform();
        transformacion.translate(desplazamientoX, desplazamientoY);
        transformacion.scale(escala, escala);
        transformacion.translate(-limites.getX(), -limites.getY());
        for (int i = 0; i < figuras.size(); i++) {
            g2.setColor(colores.get(i));
            g2.fill(transformacion.createTransformedShape(figuras.get(i)));
        }
        g2.dispose();
    }
}
