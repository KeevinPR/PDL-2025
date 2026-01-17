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
    
    // Control de errores: evita cascadas de errores en la misma linea
    private int ultimaLineaErrorLexico = -1;
    private int erroresEnLineaLexico = 0;
    private static final int MAX_ERRORES_LEXICO_POR_LINEA = 2;

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

    // Recorre todo el código y va sacando tokens
    public void analizar() throws IOException {
        char c;

        while (true) {
            c = siguienteCaracter();

            if (c == '\0') {
                // fin de fichero
                escribirToken("EOF", null);
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
                escribirToken("CODsum", null);
                continue;
            }

            if (c == '!') {
                escribirToken("CODlog", null);
                continue;
            }

            if (c == ';') {
                escribirToken("CODpc", null);
                continue;
            }

            if (c == '(') {
                escribirToken("CODparIzq", null);
                continue;
            }

            if (c == ')') {
                escribirToken("CODparDer", null);
                continue;
            }

            if (c == '{') {
                escribirToken("CODLLizq", null);
                continue;
            }

            if (c == '}') {
                escribirToken("CODLLder", null);
                continue;
            }

            if (c == ',') {
                escribirToken("CODcoma", null);
                continue;
            }

            // =, ==, %=
            if (c == '=') {
                char sig = mirarSiguiente();
                if (sig == '=') {
                    siguienteCaracter();
                    escribirToken("CODrel", null); // ==
                } else {
                    escribirToken("CODasig", null); // =
                }
                continue;
            }

            if (c == '%') {
                char sig = mirarSiguiente();
                if (sig == '=') {
                    siguienteCaracter();
                    escribirToken("CODasigRes", null); // %=
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

    // Lee entero o real
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
                    escribirToken("CODce", lexema);
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
                    escribirToken("CODcr", lexema);
                } else {
                    registrarError("real fuera de rango (debe ser menor que 117549436.0)");
                }
            } catch (NumberFormatException e) {
                registrarError("real no valido");
            }
        }
    }

    // Lee identificador o palabra reservada
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
            escribirToken("CODid", String.valueOf(handle));
        }
    }

    // Lee cadena ("...")
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
                escribirToken("CODcad", "\"" + lexema + "\"");
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

    // Escribe token (y lo guarda para el sintáctico)
    private void escribirToken(String codigo, String atributo) throws IOException {
        String attr = (atributo == null) ? "" : atributo;

        if (attr.equals("")) {
            tokOut.write("<" + codigo + ",>");
        } else {
            tokOut.write("<" + codigo + "," + attr + ">");
        }
        tokOut.newLine();

        // Guardamos también el token en memoria
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

    // códigos de palabras reservadas según la configuración de Draco
    private String codigoReservada(String lexema) {
        if (lexema.equals("let")) return "PRlet";
        if (lexema.equals("function")) return "PRfunction";
        if (lexema.equals("int")) return "PRint";
        if (lexema.equals("float")) return "PRfloat";
        if (lexema.equals("boolean")) return "PRboolean";
        if (lexema.equals("string")) return "PRstring";
        if (lexema.equals("void")) return "PRvoid";
        if (lexema.equals("if")) return "PRif";
        if (lexema.equals("for")) return "PRfor";
        if (lexema.equals("return")) return "PRreturn";
        if (lexema.equals("write")) return "PRwrite";
        if (lexema.equals("read")) return "PRread";
        return null;
    }

    // guarda ya el número de línea junto con el mensaje
    private void registrarError(String mensaje) {
        // Limitar errores por linea para evitar cascadas
        if (linea == ultimaLineaErrorLexico) {
            erroresEnLineaLexico++;
            if (erroresEnLineaLexico > MAX_ERRORES_LEXICO_POR_LINEA) {
                return; // Ignorar errores adicionales en la misma linea
            }
        } else {
            ultimaLineaErrorLexico = linea;
            erroresEnLineaLexico = 1;
        }
        
        errores.add("Linea " + linea + " (LEXICO): " + mensaje);
    }
}