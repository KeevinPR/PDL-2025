package pl.ts;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Lexer {

    private String codigo;
    private int pos;                 // posición actual en el String
    private int linea;               // número de línea para mensajes
    private BufferedWriter tokOut;   // salida de tokens
    private List<String> errores;    // lista de errores léxicos

    // lista de tokens para el sintáctico
    private List<Token> listaTokens;

    private String rutaErrores;
    
    // para no repetir muchos errores en la misma linea
    private int lineaUltimoErrorLexico = -1;
    private int contadorErrores = 0;

    public Lexer(String rutaFuente, String rutaTokens, String rutaErrores) throws IOException {
        this.codigo = leerArchivo(rutaFuente);
        this.pos = 0;
        this.linea = 1;
        // he puesto false para sobrescribir siempre el fichero de tokens, cambiar a true si queremos añadir al final
        this.tokOut = new BufferedWriter(new FileWriter(rutaTokens, false));
        this.errores = new ArrayList<String>();
        this.listaTokens = new ArrayList<Token>();
        this.rutaErrores = rutaErrores;
    }

    // funcion principal del lexico
    public void analizar() throws IOException {
        char c;

        while (true) {
            c = siguienteCaracter();

            if (c == '\0') {
                // fin de fichero
                escribirToken("cod_eof", null);
                break;
            }

            // blancos
            if (c == ' ' || c == '\t' || c == '\r') {
                continue;
            }

            // salto de línea
            if (c == '\n') {
                linea++;
                continue;
            }

            // números
            if (esDigito(c)) {
                retroceder();
                leerNumero();
                continue;
            }

            // identificadores o palabras reservadas
            if (esLetra(c) || c == '_') {
                retroceder();
                leerIdentificador();
                continue;
            }

            // cadenas con comillas dobles
            if (c == '"') {
                leerCadena();
                continue;
            }

            // comentarios de línea //
            if (c == '/') {
                char sig = mirarSiguiente();
                if (sig == '/') {
                    siguienteCaracter();
                    saltarComentario();
                } else {
                    registrarError("caracter '/' no permitido (solo comentarios //)");
                }
                continue;
            }
            

            // operadores y separadores simples
            if (c == '+') {
                escribirToken("cod_sum", null);
                continue;
            }

            if (c == '!') {
                escribirToken("cod_log", null);
                continue;
            }

            if (c == ';') {
                escribirToken("cod_pc", null);
                continue;
            }

            if (c == '(') {
                escribirToken("cod_parIzq", null);
                continue;
            }

            if (c == ')') {
                escribirToken("cod_parDer", null);
                continue;
            }

            if (c == '{') {
                escribirToken("cod_LLizq", null);
                continue;
            }

            if (c == '}') {
                escribirToken("cod_LLder", null);
                continue;
            }

            if (c == ',') {
                escribirToken("cod_coma", null);
                continue;
            }

            // =, ==, %=
            if (c == '=') {
                char sig = mirarSiguiente();
                if (sig == '=') {
                    siguienteCaracter();
                    escribirToken("cod_rel", null); // ==
                } else {
                    escribirToken("cod_asig", null); // =
                }
                continue;
            }

            if (c == '%') {
                char sig = mirarSiguiente();
                if (sig == '=') {
                    siguienteCaracter();
                    escribirToken("cod_asigRes", null); // %=
                } else {
                    registrarError("operador % sin = no permitido");
                }
                continue;
            }

            // cualquier otro carácter es error
            registrarError("caracter no reconocido: '" + c + "'");
        }

        tokOut.close();

        // Guardar errores en fichero (siempre se reinicia el fichero)
        BufferedWriter errOut = new BufferedWriter(new FileWriter(rutaErrores, false));
        for (String e : errores) {
            // e ya tiene el número de línea
            errOut.write(e);
            errOut.newLine();
        }
        errOut.close();
    }

    // Devuelve la lista de tokens al sintáctico
    public List<Token> getTokens() {
        return listaTokens;
    }

    // numeros enteros y reales
    private void leerNumero() throws IOException {
        StringBuilder sb = new StringBuilder();
        char c = siguienteCaracter();

        // parte entera
        while (esDigito(c)) {
            sb.append(c);
            c = siguienteCaracter();
        }

        boolean esReal = false;

        // parte decimal
        if (c == '.') {
            char despuesPunto = mirarSiguiente();
            if (esDigito(despuesPunto)) {
                esReal = true;
                sb.append(c); // el punto
                c = siguienteCaracter(); // primer dígito después del punto
                while (esDigito(c)) {
                    sb.append(c);
                    c = siguienteCaracter();
                }
            } else {
                registrarError("numero real mal formado (falta digito despues del punto)");
            }
        }

        // nos hemos pasado un carácter
        retroceder();

        String lexema = sb.toString();

        if (!esReal) {
            try {
                int valor = Integer.parseInt(lexema);
                if (valor < 32767) {
                    escribirToken("cod_ce", lexema);
                } else {
                    registrarError("entero fuera de rango (debe ser menor que 32767)");
                }
            } catch (NumberFormatException e) {
                registrarError("entero no valido");
            }
        } else {
            try {
                double valor = Double.parseDouble(lexema);
                if (valor < 117549436.0) {
                    escribirToken("cod_cr", lexema);
                } else {
                    registrarError("real fuera de rango (debe ser menor que 117549436.0)");
                }
            } catch (NumberFormatException e) {
                registrarError("real no valido");
            }
        }
    }

    // identificadores y palabras reservadas
    private void leerIdentificador() throws IOException {
        StringBuilder sb = new StringBuilder();
        char c = siguienteCaracter();

        while (esLetra(c) || esDigito(c) || c == '_') {
            sb.append(c);
            c = siguienteCaracter();
        }

        retroceder();

        String lexema = sb.toString();
        String codigoPR = codigoReservada(lexema);

        if (codigoPR != null) {
            escribirToken(codigoPR, null);
        } else {
            int handle = TSApi.ensureId(lexema, linea);
            escribirToken("cod_id", String.valueOf(handle));
        }
    }

    // cadenas con comillas dobles
    private void leerCadena() throws IOException {
        StringBuilder sb = new StringBuilder();
        int longitud = 0;
        boolean cerrada = false;

        char c = siguienteCaracter();

        while (c != '\0') {
            if (c == '\n') {
                registrarError("cadena no cerrada");
                linea++;
                break;
            }
            if (c == '"') {
                cerrada = true;
                break;
            }
            sb.append(c);
            longitud++;
            c = siguienteCaracter();
        }

        if (!cerrada) {
            registrarError("cadena no cerrada");
        } else {
            if (longitud < 64) {
                String lexema = sb.toString();
                escribirToken("cod_cad", "\"" + lexema + "\"");
            } else {
                registrarError("cadena demasiado larga (maximo 63 caracteres)");
            }
        }
    }

    private void saltarComentario() {
        char c = siguienteCaracter();
        while (c != '\n' && c != '\0') {
            c = siguienteCaracter();
        }
        if (c == '\n') {
            linea++;
        }
    }

    // escribir token al fichero
    private void escribirToken(String codigo, String atributo) throws IOException {
        String attr = (atributo == null) ? "" : atributo;

        if (attr.equals("")) {
            tokOut.write("<" + codigo + ",>");
        } else {
            tokOut.write("<" + codigo + "," + attr + ">");
        }
        tokOut.newLine();

        // guardar en memoria para el sintactico
        listaTokens.add(new Token(codigo, attr, linea));
    }

    private String leerArchivo(String ruta) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(ruta));
        StringBuilder sb = new StringBuilder();
        String lineaLeida;
        while ((lineaLeida = br.readLine()) != null) {
            sb.append(lineaLeida);
            sb.append('\n');
        }
        br.close();
        return sb.toString();
    }

    private char siguienteCaracter() {
        if (pos >= codigo.length()) {
            return '\0';
        }
        char c = codigo.charAt(pos);
        pos++;
        return c;
    }

    private char mirarSiguiente() {
        if (pos >= codigo.length()) {
            return '\0';
        }
        return codigo.charAt(pos);
    }

    private void retroceder() {
        if (pos > 0) {
            pos--;
        }
    }

    private boolean esDigito(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean esLetra(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    // comprobar si es palabra reservada
    private String codigoReservada(String lexema) {
        if (lexema.equals("let")) return "PR_let";
        if (lexema.equals("function")) return "PR_function";
        if (lexema.equals("int")) return "PR_int";
        if (lexema.equals("float")) return "PR_float";
        if (lexema.equals("boolean")) return "PR_boolean";
        if (lexema.equals("string")) return "PR_string";
        if (lexema.equals("void")) return "PR_void";
        if (lexema.equals("if")) return "PR_if";
        if (lexema.equals("for")) return "PR_for";
        if (lexema.equals("return")) return "PR_return";
        if (lexema.equals("write")) return "PR_write";
        if (lexema.equals("read")) return "PR_read";
        return null;
    }

    // guardar error
    private void registrarError(String mensaje) {
        // evitar demasiados errores en la misma linea
        if (linea == lineaUltimoErrorLexico) {
            contadorErrores++;
            if (contadorErrores > 2) {
                return;
            }
        } else {
            lineaUltimoErrorLexico = linea;
            contadorErrores = 1;
        }
        
        errores.add("Linea " + linea + " (LEXICO): " + mensaje);
    }
}