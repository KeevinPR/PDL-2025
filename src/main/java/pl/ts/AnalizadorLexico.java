package pl.ts;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// El Lexer se encarga de trocear el codigo en tokens
public class AnalizadorLexico {

    private String codigo;
    private int pos;                 // por donde vamos leyendo el string
    private int linea;               // para saber en que linea estamos si hay error
    private BufferedWriter tokOut;   // donde escribimos los tokens
    private List<String> errores;    // para ir guardando los errores que salgan
    private List<Token> listaTokens; // lista para pasarle luego al parser
    private String rutaErrores;
    
    // para no poner mil errores si en una linea todo esta mal
    private int lineaUltimoErrorLexico = -1;
    private int contadorErrores = 0;

    public AnalizadorLexico(String rutaFuente, String rutaTokens, String rutaErrores) throws IOException {
        this.codigo = leerArchivo(rutaFuente);
        this.pos = 0;
        this.linea = 1;
        this.tokOut = new BufferedWriter(new FileWriter(rutaTokens, false));
        this.errores = new ArrayList<String>();
        this.listaTokens = new ArrayList<Token>();
        this.rutaErrores = rutaErrores;
    }

    // El metodo principal que recorre todo el fichero
    public void analizar() throws IOException {
        char c;

        while (true) {
            c = siguienteCaracter();

            if (c == '\0') {
                // fin del archivo, metemos el token de EOF
                escribirToken("cod_eof", null);
                break;
            }

            // ignoramos espacios, tabuladores y retornos de carro
            if (c == ' ' || c == '\t' || c == '\r') {
                continue;
            }

            // si es un salto de linea, sumamos 1 al contador
            if (c == '\n') {
                linea++;
                continue;
            }

            // si es un numero, vamos a leerlo entero
            if (esDigito(c)) {
                retroceder();
                leerNumero();
                continue;
            }

            // si empieza por letra o _, puede ser id o palabra reservada
            if (esLetra(c) || c == '_') {
                retroceder();
                leerIdentificador();
                continue;
            }

            // si empieza por comillas, es una cadena
            if (c == '"') {
                leerCadena();
                continue;
            }

            // comentarios que empiezan por // (solo de una linea)
            if (c == '/') {
                char sig = mirarSiguiente();
                if (sig == '/') {
                    siguienteCaracter(); // saltamos el segundo /
                    saltarComentario();
                } else {
                    registrarError("caracter '/' no permitido (solo comentarios //)");
                }
                continue;
            }
            

            // Simbolos simples
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

            // comprobamos si es = o ==
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

            // comprobamos si es %=
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

            // si llegamos aqui es que no sabemos que es
            registrarError("caracter no reconocido: '" + c + "'");
        }

        tokOut.close();

        // Al final, guardamos todos los errores en su fichero
        BufferedWriter errOut = new BufferedWriter(new FileWriter(rutaErrores, false));
        for (String e : errores) {
            errOut.write(e);
            errOut.newLine();
        }
        errOut.close();
    }

    // para que el parser pueda pedirnos los tokens mas tarde
    public List<Token> getTokens() {
        return listaTokens;
    }

    // funcion para leer numeros (pueden ser 123 o 12.34)
    private void leerNumero() throws IOException {
        StringBuilder sb = new StringBuilder();
        char c = siguienteCaracter();

        // parte de delante del punto
        while (esDigito(c)) {
            sb.append(c);
            c = siguienteCaracter();
        }

        boolean esReal = false;

        // si hay un punto, miramos si vienen mas numeros
        if (c == '.') {
            char despuesPunto = mirarSiguiente();
            if (esDigito(despuesPunto)) {
                esReal = true;
                sb.append(c); 
                c = siguienteCaracter(); 
                while (esDigito(c)) {
                    sb.append(c);
                    c = siguienteCaracter();
                }
            } else {
                registrarError("numero real mal formado (falta digito despues del punto)");
            }
        }

        // volvemos un paso atras para no comernos el siguiente token
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
                // limite que nos han puesto en la documentacion
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

    // para leer variables o palabras como function, let...
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

        // si es palabra reservada mandamos su codigo, si no, es un ID normal
        if (codigoPR != null) {
            escribirToken(codigoPR, null);
        } else {
            // si es un ID, lo metemos en la tabla de simbolos
            int handle = TablaSimbolos.gestionarId(lexema);
            escribirToken("cod_id", String.valueOf(handle));
        }
    }

    // lee cadenas que van entre " "
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
            // maximo 64 caracteres
            if (longitud < 64) {
                String lexema = sb.toString();
                escribirToken("cod_cad", "\"" + lexema + "\"");
            } else {
                registrarError("cadena demasiado larga (maximo 63 caracteres)");
            }
        }
    }

    // se salta lo que hay detras de // hasta el final de linea
    private void saltarComentario() {
        char c = siguienteCaracter();
        while (c != '\n' && c != '\0') {
            c = siguienteCaracter();
        }
        if (c == '\n') {
            linea++;
        }
    }

    // para escribir el token tanto en el fichero como en la lista de memoria
    private void escribirToken(String codigo, String atributo) throws IOException {
        String attr = (atributo == null) ? "" : atributo;

        if (attr.equals("")) {
            tokOut.write("<" + codigo + ",>");
        } else {
            tokOut.write("<" + codigo + "," + attr + ">");
        }
        tokOut.newLine();

        listaTokens.add(new Token(codigo, attr, linea));
    }

    // lee todo el fichero JS de golpe a un String
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

    // mapeo de palabras reservadas a sus codigos
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

    // guarda el error y controla que no salgan demasiados seguidos
    private void registrarError(String mensaje) {
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
