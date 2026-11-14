package com.mycompany.t1operativos.modelo;

/**
 * Representa la CPU de la Mini PC.
 *
 * Mantiene los registros y ejecuta la instrucción que recibe. La lectura de
 * instrucciones desde memoria corresponde al gestor de procesos.
 *
 * @author Deislher Sánchez Funez
 * @author Natalia Granados Rosales
 */
public class CPU {
    private int pc;
    private String[] ir;
    private int ac;
    private int ax;
    private int bx;
    private int cx;
    private String dx;
    private int ah;
    private int al;
    private boolean flag;
    private int pila1;
    private int pila2;
    private int pila3;
    private int pila4;
    private int pila5;
    private int punteroPila;
    private int inicioMemoria;
    private int finMemoria;
    private String interrupcion;

    /**
     * Construye una CPU e inicializa sus registros.
     */
    public CPU() {
        reiniciar();
    }

    /**
     * Carga los registros del contexto.
     *
     * @param pc contador de programa.
     * @param ir instrucción guardada, o null.
     * @param ac acumulador.
     * @param ax registro AX.
     * @param bx registro BX.
     * @param cx registro CX.
     * @param dx registro DX.
     * @param ah registro AH.
     * @param al registro AL.
     * @param flag resultado de la comparación.
     */
    public void cargarContexto(int pc, String[] ir, int ac, int ax, int bx, int cx, String dx, int ah, int al, boolean flag) {
        this.pc = pc;
        this.ir = ir;
        this.ac = ac;
        this.ax = ax;
        this.bx = bx;
        this.cx = cx;
        this.dx = dx;
        this.ah = ah;
        this.al = al;
        this.flag = flag;
    }

    /**
     * Carga la pila del contexto.
     *
     * @param pila1 valor de la posición 1.
     * @param pila2 valor de la posición 2.
     * @param pila3 valor de la posición 3.
     * @param pila4 valor de la posición 4.
     * @param pila5 valor de la posición 5.
     * @param punteroPila posición del tope, o -1 si está vacía.
     */
    public void cargarPila(int pila1, int pila2, int pila3, int pila4, int pila5, int punteroPila) {
        this.pila1 = pila1;
        this.pila2 = pila2;
        this.pila3 = pila3;
        this.pila4 = pila4;
        this.pila5 = pila5;
        this.punteroPila = punteroPila;
    }

    /**
     * Carga los límites del programa en ejecución.
     *
     * @param inicio primera posición del programa.
     * @param fin última posición del programa.
     */
    public void cargarLimites(int inicio, int fin) {
        this.inicioMemoria = inicio;
        this.finMemoria = fin;
    }

    /**
     * Carga en el IR la instrucción que se va a ejecutar.
     *
     * @param instruccion arreglo con el operador y sus operandos.
     */
    public void cargarInstruccion(String[] instruccion) {
        if (instruccion == null || instruccion.length == 0) {
            throw new IllegalArgumentException("La instrucción no puede ser nula ni vacía.");
        }
        ir = instruccion;
    }

    /**
     * Ejecuta una instrucción validada por el parser.
     *
     * @param instruccion arreglo con la instrucción que se desea ejecutar.
     */
    public void ejecutarInstruccion(String[] instruccion) {
        cargarInstruccion(instruccion);
        interrupcion = null;
        int siguientePc = pc + 1;
        String operador = ir[0];
        switch (operador) {
            case "MOV":
                ejecutarMov(ir[1], ir[2]);
                break;
            case "LOAD":
                ac = leerRegistro(ir[1]);
                break;
            case "STORE":
                escribirRegistro(ir[1], ac);
                break;
            case "ADD":
                ac = ac + leerRegistro(ir[1]);
                break;
            case "SUB":
                ac = ac - leerRegistro(ir[1]);
                break;
            case "INC":
                if (ir[1].isEmpty()) {
                    ac++;
                } else {
                    escribirRegistro(ir[1], leerRegistro(ir[1]) + 1);
                }
                break;
            case "DEC":
                if (ir[1].isEmpty()) {
                    ac--;
                } else {
                    escribirRegistro(ir[1], leerRegistro(ir[1]) - 1);
                }
                break;
            case "SWAP":
                int primerValor = leerRegistro(ir[1]);
                int segundoValor = leerRegistro(ir[2]);
                escribirRegistro(ir[1], segundoValor);
                escribirRegistro(ir[2], primerValor);
                break;
            case "CMP":
                flag = leerRegistro(ir[1]) == leerRegistro(ir[2]);
                break;
            case "JMP":
                siguientePc = calcularDestinoSalto(Integer.parseInt(ir[1]));
                break;
            case "JE":
                if (flag) {
                    siguientePc = calcularDestinoSalto(Integer.parseInt(ir[1]));
                }
                break;
            case "JNE":
                if (!flag) {
                    siguientePc = calcularDestinoSalto(Integer.parseInt(ir[1]));
                }
                break;
            case "PARAM":
                int cantidad = ir.length - 1;
                if (punteroPila + 1 + cantidad > 5) {
                    throw new IllegalStateException("Desbordamiento de pila: no hay espacio para los " + cantidad + " parámetros. Capacidad máxima de 5 valores.");
                }
                for (int i = 1; i < ir.length; i++) {
                    apilar(Integer.parseInt(ir[i]));
                }
                break;
            case "PUSH":
                apilar(leerRegistro(ir[1]));
                break;
            case "POP":
                escribirRegistro(ir[1], desapilar());
                break;
            case "INT":
                interrupcion = ir[1];
                break;
            default:
                throw new IllegalArgumentException("La instrucción " + operador + " todavía no está implementada en la CPU.");
        }
        pc = siguientePc;
    }

    /**
     * Calcula la dirección de un salto dentro del programa.
     *
     * @param desplazamiento desplazamiento desde PC.
     * @return destino del salto.
     */
    private int calcularDestinoSalto(int desplazamiento) {
        int destino = pc + desplazamiento;
        if (destino < inicioMemoria || destino > finMemoria) {
            throw new IllegalStateException("Salto fuera de los límites del programa: dirección " + destino + ". Rango permitido: " + inicioMemoria + " a " + finMemoria + ".");
        }
        return destino;
    }

    /**
     * Guarda un valor en el tope de la pila.
     *
     * @param valor valor que se desea apilar.
     */
    private void apilar(int valor) {
        if (punteroPila >= 4) {
            throw new IllegalStateException("Desbordamiento de pila: capacidad máxima de 5 valores.");
        }
        punteroPila++;
        setValorPila(punteroPila, valor);
    }

    /**
     * Retira el valor del tope de la pila.
     *
     * @return valor retirado.
     */
    private int desapilar() {
        if (punteroPila < 0) {
            throw new IllegalStateException("La pila está vacía.");
        }
        int valor = getValorPila(punteroPila);
        setValorPila(punteroPila, 0);
        punteroPila--;
        return valor;
    }

    /**
     * Obtiene el valor de una posición de la pila.
     *
     * @param posicion índice de la posición, de 0 a 4.
     * @return valor almacenado en esa posición.
     */
    public int getValorPila(int posicion) {
        switch (posicion) {
            case 0:
                return pila1;
            case 1:
                return pila2;
            case 2:
                return pila3;
            case 3:
                return pila4;
            case 4:
                return pila5;
            default:
                throw new IllegalArgumentException("Posición de pila inválida: " + posicion);
        }
    }

    /**
     * Actualiza una posición de la pila.
     *
     * @param posicion índice de la posición, de 0 a 4.
     * @param valor valor que se desea almacenar.
     */
    private void setValorPila(int posicion, int valor) {
        switch (posicion) {
            case 0:
                pila1 = valor;
                break;
            case 1:
                pila2 = valor;
                break;
            case 2:
                pila3 = valor;
                break;
            case 3:
                pila4 = valor;
                break;
            case 4:
                pila5 = valor;
                break;
            default:
                throw new IllegalArgumentException("Posición de pila inválida: " + posicion);
        }
    }

    /**
     * Obtiene la posición del tope de la pila.
     *
     * @return posición del tope, o -1 si la pila está vacía.
     */
    public int getPunteroPila() {
        return punteroPila;
    }

    /**
     * Obtiene la interrupción generada por la última instrucción.
     *
     * @return código de la interrupción, o null.
     */
    public String getInterrupcion() {
        return interrupcion;
    }

    /**
     * Ejecuta un MOV validado por el parser.
     *
     * @param registro registro destino.
     * @param origen registro o valor de origen.
     */
    private void ejecutarMov(String registro, String origen) {
        if ("AH".equals(registro)) {
            switch (origen) {
                case "3CH":
                    ah = 60;
                    break;
                case "3DH":
                    ah = 61;
                    break;
                case "4DH":
                    ah = 77;
                    break;
                case "40H":
                    ah = 64;
                    break;
                case "41H":
                    ah = 65;
                    break;
            }
            return;
        }
        if ("AL".equals(registro)) {
            if (esRegistro(origen)) {
                al = leerRegistro(origen);
            } else if (!"AL".equals(origen)) {
                al = Integer.parseInt(origen);
            }
            return;
        }
        if ("DX".equals(registro)) {
            if ("AL".equals(origen)) {
                dx = Integer.toString(al);
                return;
            }
            if ("DX".equals(origen)) {
                return;
            }
            if (origen.startsWith("\"")) {
                dx = origen.substring(1, origen.length() - 1);
                return;
            }
        }
        int valor;
        if (esRegistro(origen)) {
            valor = leerRegistro(origen);
        } else {
            valor = Integer.parseInt(origen);
        }
        escribirRegistro(registro, valor);
    }

    /**
     * Avanza el contador de programa a la siguiente posición.
     */
    public void avanzarPc() {
        pc++;
    }

    /**
     * Lee el valor numérico de un registro.
     *
     * @param registro nombre del registro que se desea leer.
     * @return el valor del registro.
     */
    public int leerRegistro(String registro) {
        switch (registro) {
            case "AX":
                return ax;
            case "BX":
                return bx;
            case "CX":
                return cx;
            case "DX":
                try {
                    return Integer.parseInt(dx);
                } catch (NumberFormatException e) {
                    throw new IllegalStateException("DX contiene texto y no puede usarse como número: " + dx);
                }
            default:
                throw new IllegalArgumentException("Registro desconocido: " + registro);
        }
    }

    /**
     * Almacena un valor numérico en un registro.
     *
     * @param registro nombre del registro que se desea actualizar.
     * @param valor valor que se desea almacenar.
     */
    public void escribirRegistro(String registro, int valor) {
        switch (registro) {
            case "AX":
                ax = valor;
                break;
            case "BX":
                bx = valor;
                break;
            case "CX":
                cx = valor;
                break;
            case "DX":
                dx = Integer.toString(valor);
                break;
            default:
                throw new IllegalArgumentException("Registro desconocido: " + registro);
        }
    }

    /**
     * Comprueba si un valor corresponde a un registro.
     *
     * @param valor nombre que se desea comprobar.
     * @return {@code true} si el valor es AX, BX, CX o DX.
     */
    private boolean esRegistro(String valor) {
        boolean esValido = "AX".equals(valor) || "BX".equals(valor) || "CX".equals(valor) || "DX".equals(valor);
        return esValido;
    }

    /**
     * Reinicia los registros de la CPU.
     */
    public void reiniciar() {
        pc = 0;
        ir = null;
        ac = 0;
        ax = 0;
        bx = 0;
        cx = 0;
        dx = "";
        ah = 0;
        al = 0;
        flag = false;
        pila1 = 0;
        pila2 = 0;
        pila3 = 0;
        pila4 = 0;
        pila5 = 0;
        punteroPila = -1;
        inicioMemoria = 0;
        finMemoria = 0;
        interrupcion = null;
    }

    /**
     * Obtiene el contador de programa actual.
     *
     * @return el valor del PC.
     */
    public int getPc() {
        return pc;
    }

    /**
     * Cambia el valor del contador de programa.
     *
     * @param valor posición de memoria que se desea asignar al PC.
     */
    public void setPc(int valor) {
        if (valor < 0) {
            throw new IllegalArgumentException("El PC no puede ser negativo.");
        }
        pc = valor;
    }

    /**
     * Obtiene la instrucción actual.
     *
     * @return el IR, o {@code null} si no hay una instrucción.
     */
    public String[] getIr() {
        return ir;
    }

    /**
     * Obtiene el valor actual del acumulador.
     *
     * @return el valor de AC.
     */
    public int getAc() {
        return ac;
    }

    /**
     * Obtiene el valor actual del registro AX.
     *
     * @return el valor de AX.
     */
    public int getAx() {
        return ax;
    }

    /**
     * Obtiene el valor actual del registro BX.
     *
     * @return el valor de BX.
     */
    public int getBx() {
        return bx;
    }

    /**
     * Obtiene el valor actual del registro CX.
     *
     * @return el valor de CX.
     */
    public int getCx() {
        return cx;
    }

    /**
     * Obtiene el contenido actual del registro DX.
     *
     * @return el contenido de DX.
     */
    public String getDx() {
        return dx;
    }

    /**
     * Obtiene el código de servicio de AH.
     *
     * @return el valor de AH.
     */
    public int getAh() {
        return ah;
    }

    /**
     * Obtiene el valor de AL.
     *
     * @return el valor de AL.
     */
    public int getAl() {
        return al;
    }

    /**
     * Comprueba el resultado de la última comparación.
     *
     * @return {@code true} si los registros comparados eran iguales.
     */
    public boolean esIgual() {
        return flag;
    }

    /**
     * Asigna texto al registro DX.
     *
     * @param valor texto que se desea almacenar.
     */
    public void setDx(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("DX no puede ser nulo.");
        }
        dx = valor;
    }

    /**
     * Asigna un entero a AL.
     *
     * @param valor entero que se guardará.
     */
    public void setAl(int valor) {
        al = valor;
    }
}
