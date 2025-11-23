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

    public Lexer(String rutaFuente, String rutaTokens) throws IOException {
        this.codigo = leerArchivo(rutaFuente);
        this.pos = 0;
        this.linea = 1;
        // he puesto false para sobrescribir siempre el fichero de tokens, cambiar a true si queremos añadir al final
        this.tokOut = new BufferedWriter(new FileWriter(rutaTokens, false));
        this.errores = new ArrayList<String>();
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
                    siguienteCaracter(); // consumimos el segundo '/'
                    saltarComentario();
                    continue;
                } else {
                    registrarError("se esperaba // para comentario");
                    continue;
                }
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
            registrarError("carácter no reconocido: '" + c + "'");
        }

        tokOut.close();

        // Guardar errores en fichero (si hay)
        if (!errores.isEmpty()) {
            BufferedWriter errOut = new BufferedWriter(new FileWriter("errores_lexicos.txt", false));
            for (String e : errores) {
                errOut.write("Linea " + linea + " (LEXICO): " + e);
                errOut.newLine();
            }
            errOut.close();
        }
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
                registrarError("número real mal formado (falta dígito después del punto)");
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
                    registrarError("entero no válido (mayor o igual que 32767)");
                }
            } catch (NumberFormatException e) {
                registrarError("entero no válido");
            }
        } else {
            try {
                double valor = Double.parseDouble(lexema);
                if (valor < 117549436.0) {
                    escribirToken("CODcr", lexema);
                } else {
                    registrarError("real no válido (mayor o igual que 117549436.0)");
                }
            } catch (NumberFormatException e) {
                registrarError("real no válido");
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
                registrarError("cadena no válida (longitud mayor o igual que 64)");
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

    // Escribe token
    private void escribirToken(String codigo, String atributo) throws IOException {
        if (atributo == null || atributo.equals("")) {
            tokOut.write("<" + codigo + ",>");
        } else {
            tokOut.write("<" + codigo + "," + atributo + ">");
        }
        tokOut.newLine();
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

    private void registrarError(String mensaje) {
        errores.add(mensaje);
    }
}