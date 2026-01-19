package pl.ts;

import java.io.*;
import java.util.List;

// El Analizador Sintactico (Parser)
// Usamos el metodo descendente LL(1)
public class AnalizadorSintactico {

    private List<Token> tokens;
    private int pos;
    private Token actual;

    private BufferedWriter parseOut; // para guardar los numeros de las reglas
    private BufferedWriter errOut;   // para guardar los errores sintacticos y semanticos
    
    // para no dar mil errores en la misma linea
    private int ultimaLineaError = -1;
    private int erroresEnLinea = 0;
    private static final int MAX_ERRORES_POR_LINEA = 1;

    // estas variables nos sirven para la tabla de simbolos
    private int desp = 0;
    private boolean zonaDeclaracion = false;

    public AnalizadorSintactico(List<Token> tokens, String rutaParse, String rutaErrores) throws IOException {
        this.tokens = tokens;
        this.pos = 0;
        if (tokens.size() > 0) {
            this.actual = tokens.get(0);
        } else {
            this.actual = null;
        }
        this.parseOut = new BufferedWriter(new FileWriter(rutaParse, false));

        // Ponemos la D al principio del fichero de parse
        parseOut.write("D");
        parseOut.newLine();

        // Usamos append para no borrar lo que haya escrito el lexer
        this.errOut = new BufferedWriter(new FileWriter(rutaErrores, true));
    }

    public void analizar() throws IOException {
        if (actual == null) {
            cerrar();
            return;
        }

        // empezamos por el simbolo inicial de la gramatica (P)
        P(); 

        // si sobra algo despues del final del programa es error
        if (actual != null && !"cod_eof".equals(actual.codigo)) {
            error("sobra codigo despues del final del programa");
        }

        cerrar();
    }

    private void cerrar() throws IOException {
        parseOut.close();
        errOut.close();
    }

    // Funciones que nos ayudan a mirar que token tenemos
    private boolean es(String codigo) {
        return actual != null && codigo.equals(actual.codigo);
    }
    
    // para sacar el nombre real (lexema) de un ID usando su numero
    private String getLexema(Token t) {
        if (t == null) return "";
        try {
            int h = Integer.parseInt(t.atributo);
            TablaSimbolos.Simbolo s = TablaSimbolos.getSimbolo(h);
            return s.lexema;
        } catch (Exception e) {
            return "";
        }
    }

    // para saber cuanto ocupa cada tipo en memoria
    private int getAncho(String tipo) {
        if ("cadena".equals(tipo)) return 64;
        return 1; // int, float y boolean ocupan 1 unidad
    }

    // pasar al siguiente token de la lista
    private void avanzar() {
        if (pos < tokens.size() - 1) {
            pos++;
            actual = tokens.get(pos);
        } else {
            actual = tokens.get(tokens.size() - 1); // nos quedamos en el EOF
        }
    }

    // comprobamos que el token es el que esperamos y avanzamos
    private void match(String esperado) throws IOException {
        if (es(esperado)) {
            avanzar();
        } else {
            error("se esperaba " + nombreLegible(esperado) + " y se encontro " +
                  nombreLegible(actual == null ? null : actual.codigo) + pista(esperado));
            avanzar();
        }
    }

    // pequeños mensajes extra para ayudar
    private String pista(String esperado) {
        switch (esperado) {
            case "cod_pc": return " (falta ';' al final?)";
            case "cod_parDer": return " (revisa los parentesis)";
            case "cod_LLder": return " (falta '}' de cierre?)";
            case "cod_LLizq": return " (falta '{' de apertura?)";
            default: return "";
        }
    }

    // apunta el numero de regla que hemos usado
    private void regla(int n) throws IOException {
        parseOut.write(Integer.toString(n));
        parseOut.newLine();
    }

    private void error(String msg) throws IOException {
        error(msg, "SINTACTICO");
    }

    private void errorSemantico(String msg) throws IOException {
        error(msg, "SEMANTICO");
    }

    // para escribir el error en el fichero errores.txt
    private void error(String msg, String tipo) throws IOException {
        int linea = (actual == null ? -1 : actual.linea);
        
        if (linea == ultimaLineaError) {
            erroresEnLinea++;
            if (erroresEnLinea > MAX_ERRORES_POR_LINEA) {
                return; 
            }
        } else {
            ultimaLineaError = linea;
            erroresEnLinea = 1;
        }
        
        if (linea == -1) {
            errOut.write("Linea ? (" + tipo + "): " + msg);
        } else {
            errOut.write("Linea " + linea + " (" + tipo + "): " + msg);
        }
        errOut.newLine();
    }

    // devuelve un nombre mas legible para los codigos internos
    private String nombreLegible(String codigo) {
        if (codigo == null) return "fin de fichero";
        switch (codigo) {
            case "cod_pc": return "';'";
            case "cod_parIzq": return "'('";
            case "cod_parDer": return "')'";
            case "cod_LLizq": return "'{'";
            case "cod_LLder": return "'}'";
            case "cod_id": return "identificador";
            case "cod_cad": return "cadena";
            case "cod_ce": return "entero";
            case "cod_cr": return "real";
            case "cod_asig": return "'='";
            case "cod_asigRes": return "'%='";
            case "cod_coma": return "','";
            case "cod_sum": return "'+'";
            case "cod_rel": return "'=='";
            case "cod_log": return "'!'";
            case "PR_let": return "'let'";
            case "PR_function": return "'function'";
            case "PR_int": return "'int'";
            case "PR_float": return "'float'";
            case "PR_boolean": return "'boolean'";
            case "PR_string": return "'string'";
            case "PR_void": return "'void'";
            case "PR_if": return "'if'";
            case "PR_for": return "'for'";
            case "PR_return": return "'return'";
            case "PR_write": return "'write'";
            case "PR_read": return "'read'";
            case "cod_eof": return "fin de fichero";
            default: return codigo;
        }
    }

    // Auxiliares para no repetir condiciones largas de tipos o sentencias

    private boolean esTipo() {
        return es("PR_int") || es("PR_float") || es("PR_boolean") || es("PR_string");
    }

    private boolean esNumerico(String t) {
        return "entero".equals(t) || "real".equals(t);
    }

    private boolean esInicioSentencia() {
        return es("cod_id") || es("PR_for") || es("PR_if") || es("PR_read")
                || es("PR_write") || es("PR_return") || es("cod_LLizq")
                || es("cod_pc") || es("PR_let");
    }

    private boolean esInicioExpr() {
        return es("cod_log") || es("cod_parIzq") || es("cod_cad")
                || es("cod_ce") || es("cod_id") || es("cod_cr");
    }

    private boolean esInicioPrimario() {
        return es("cod_id") || es("cod_ce") || es("cod_cr")
                || es("cod_cad") || es("cod_parIzq");
    }

    // REGLAS DE LA GRAMATICA
    // 1: P  -> G eof
    // 2: G  -> E G | 3: λ
    // 4: E  -> D | 5: F | 6: S
    // 7: D  -> let T id D1 ;
    // 8: D1 -> = X | 9: λ
    // 10: T  -> int | 11: float | 12: boolean | 13: string
    // 14: F  -> function R id ( PO ) B
    // 15: R  -> T | 16: void
    // 17: PO -> PL | 18: λ
    // 19: PL -> PA LP
    // 20: LP -> , PA LP | 21: λ
    // 22: PA -> T id
    // 23: B  -> { LS }
    // 24: LS -> S LS | 25: λ
    // 26: S  -> SA | 27: SF | 28: SI | 29: SR | 30: SW | 31: ST | 32: B | 33: ;
    // 34: SA -> id OP X ;
    // 35: OP -> = | 36: %=
    // 37: SF -> for ( F0 ; F1 ; F2 ) S
    // 38: F0 -> id OP X | 39: let T id D1 | 40: λ
    // 41: F1 -> X | 42: λ
    // 43: F2 -> id OP X | 44: λ
    // 45: SI -> if ( X ) S
    // 46: SR -> read ( id ) ;
    // 47: SW -> write ( X ) ;
    // 48: ST -> return X0 ;
    // 49: X0 -> X | 50: λ
    // 51: X  -> X1
    // 52: X1 -> X2 X1'
    // 53: X1'-> == X2 X1' | 54: λ
    // 55: X2 -> X3 X2'
    // 56: X2'-> + X3 X2' | 57: λ
    // 58: X3 -> ! X3 | 59: V
    // 60: V  -> id V' | 61: entero | 62: real | 63: cadena | 64: ( X )
    // 65: V' -> ( AO ) | 66: λ
    // 67: AO -> AL | 68: λ
    // 69: AL -> X AL'
    // 70: AL'-> , X AL' | 71: λ

    private void P() throws IOException {
        regla(1);
        G();
        match("cod_eof");
    }

    private void G() throws IOException {
        if (esInicioSentencia() || es("PR_let") || es("PR_function")) {
            regla(2);
            E();
            G();
        } else if (es("cod_eof")) {
            regla(3);
        } else {
            error("se esperaba una declaracion, funcion o sentencia");
        }
    }

    private void E() throws IOException {
        if (es("PR_let")) {
            regla(4);
            D();
        } else if (es("PR_function")) {
            regla(5);
            F();
        } else if (esInicioSentencia()) {
            regla(6);
            S();
        } else {
            error("se esperaba 'let', 'function' o una sentencia");
        }
    }

    // Declaracion de variables con let
    private void D() throws IOException {
        if (!es("PR_let")) {
            error("se esperaba 'let' para declarar variable");
        }
        regla(7);
        match("PR_let");
        
        zonaDeclaracion = true; // para saber que estamos declarando
        String tipo = T();
        
        Token tId = actual;
        String lexema = getLexema(tId); 
        match("cod_id");

        // Metemos la variable en la tabla de simbolos
        if (!lexema.isEmpty()) {
            int handle = TablaSimbolos.gestionarId(lexema);
            TablaSimbolos.Simbolo s = TablaSimbolos.getSimbolo(handle);
            if (s != null) {
                // si el tipo no es -, es que ya estaba declarada antes
                if (!"-".equals(s.tipo)) {
                    errorSemantico("variable '" + lexema + "' ya declarada en este ambito");
                } else {
                    s.tipo = tipo;
                    s.desp = desp;
                    desp += getAncho(tipo);
                }
            }
        }
        zonaDeclaracion = false;

        D1(tipo); // por si tiene un = detras
        match("cod_pc");
    }

    // Parte opcional de la declaracion ( = valor )
    private void D1(String tipoLhs) throws IOException {
        if (es("cod_asig")) {
            regla(8);
            match("cod_asig");
            String tipoRhs = X(); // miramos que tipo es lo de la derecha
            if (!"error".equals(tipoLhs) && !"error".equals(tipoRhs)) {
                if (!tipoLhs.equals(tipoRhs)) {
                    errorSemantico("tipo incorrecto en inicializacion. Se esperaba " + tipoLhs + " pero se encontro " + tipoRhs);
                }
            }
        } else if (es("cod_pc")) {
            regla(9); // no hay asignacion inicial
        } else {
            error("se esperaba '=' o ';' en la declaracion");
        }
    }

    // Para leer el tipo de la variable
    private String T() throws IOException {
        if (es("PR_int")) {
            regla(10);
            match("PR_int");
            return "entero";
        } else if (es("PR_float")) {
            regla(11);
            match("PR_float");
            return "real";
        } else if (es("PR_boolean")) {
            regla(12);
            match("PR_boolean");
            return "boolean";
        } else if (es("PR_string")) {
            regla(13);
            match("PR_string");
            return "cadena";
        } else {
            error("se esperaba un tipo: int, float, boolean o string");
            return "error";
        }
    }

    // Definicion de funciones
    private void F() throws IOException {
        regla(14);
        match("PR_function");
        String retType = R(); // tipo de retorno
        Token tId = actual;
        String lexema = getLexema(tId);
        match("cod_id");
        
        // Guardamos la funcion en la tabla global (actualmente estamos en global)
        TablaSimbolos.Simbolo funcion = null;
        if (!lexema.isEmpty()) {
            int h = TablaSimbolos.gestionarId(lexema);
            funcion = TablaSimbolos.getSimbolo(h);
            if(funcion != null) {
                funcion.tipo = retType;
                funcion.tipoRetorno = retType;
            }
        }
        
        // Entramos en el nuevo ambito de la funcion
        TablaSimbolos.entrarBloque(lexema);
        int oldDesp = desp;
        desp = 0; // el desplazamiento local empieza en 0
        
        match("cod_parIzq");
        PO(); // parametros opcionales
        match("cod_parDer");

        // Rellenamos los datos de la funcion con sus parametros
        if (funcion != null) {
            TablaSimbolos.Tabla tablaLocal = TablaSimbolos.getTablaActual();
            funcion.numParams = tablaLocal.simbolos.size();
            for(int i=0; i<funcion.numParams; i++) {
                if (i < 10) { 
                    funcion.tipoParam[i] = tablaLocal.simbolos.get(i).tipo;
                    funcion.modoParam[i] = "valor"; 
                }
            }
        }



        String tipoEncontrado = B(); // cuerpo de la funcion { ... }
        
        // miramos si lo que devuelve el cuerpo coincide con lo que pide la funcion
        if (!tipoEncontrado.equals(retType)) {
             errorSemantico("Retorno no esperado");
        }
        
        // Salimos del ambito y recuperamos el desplazamiento de antes
        desp = oldDesp;
        TablaSimbolos.salirBloque();
    }

    // Tipo de retorno de funcion (incluye void)
    private String R() throws IOException {
        if (esTipo()) {
            regla(15);
            return T();
        } else if (es("PR_void")) {
            regla(16);
            match("PR_void");
            return "void";
        } else {
            error("se esperaba un tipo (int, float, boolean, string) o 'void'");
            return "error";
        }
    }

    // Parametros opcionales
    private void PO() throws IOException {
        if (esTipo()) {
            regla(17);
            PL();
        } else if (es("cod_parDer")) {
            regla(18); // no hay parametros
        } else {
            error("parametro incorrecto: se esperaba un tipo o ')' para cerrar");
        }
    }

    // Lista de parametros
    private void PL() throws IOException {
        regla(19);
        PA();
        LP();
    }

    // Continuacion de lista de parametros con coma
    private void LP() throws IOException {
        if (es("cod_coma")) {
            regla(20);
            match("cod_coma");
            PA();
            LP();
        } else if (es("cod_parDer")) {
            regla(21); // final de la lista
        } else {
            error("se esperaba ',' o ')' en la lista de parametros");
        }
    }

    // Un parametro individual tipo id
    private void PA() throws IOException {
        regla(22);
        String tipo = T();
        Token tId = actual;
        String lexema = getLexema(tId);
        match("cod_id");
        
        // El parametro va a la tabla de simbolos local
        if(!lexema.isEmpty()) {
            int h = TablaSimbolos.gestionarId(lexema);
            TablaSimbolos.Simbolo s = TablaSimbolos.getSimbolo(h);
            if(s!=null) {
                s.tipo = tipo;
                s.desp = desp; 
                s.esParametro = 1; // marcamos que es un parametro
                desp += getAncho(tipo); 
            }
        }
    }

    // Bloque entre llaves { ... }
    private String B() throws IOException {
        regla(23);
        match("cod_LLizq");
        String t = LS();
        match("cod_LLder");
        return t;
    }

    // Lista de sentencias dentro de un bloque
    private String LS() throws IOException {
        if (esInicioSentencia()) {
            regla(24);
            String tipoS = S();
            String tipoLS = LS();
            // si la sentencia S devuelve algo (no void), nos quedamos con eso
            if (!"void".equals(tipoS)) return tipoS;
            // si no, lo que diga el resto de la lista
            return tipoLS;
        } else if (es("cod_LLder")) {
            regla(25); // final del bloque
            return "void";
        } else {
            error("se esperaba una sentencia o '}' para cerrar el bloque");
            return "void";
        }
    }

    // Una sentencia cualquiera
    private String S() throws IOException {
        if (es("cod_id")) {
            regla(26);
            SA();
            return "void";
        } else if (es("PR_for")) {
            regla(27);
            SF();
            return "void";
        } else if (es("PR_if")) {
            regla(28);
            SI();
            return "void";
        } else if (es("PR_read")) {
            regla(29);
            SR();
            return "void";
        } else if (es("PR_write")) {
            regla(30);
            SW();
            return "void";
        } else if (es("PR_return")) {
            regla(31);
            return ST();
        } else if (es("cod_LLizq")) {
            regla(32);
            return B();
        } else if (es("cod_pc")) {
            regla(34);
            match("cod_pc");
            return "void";
        } else if (es("PR_let")) {
            regla(33);
            D();
            return "void";
        } else {
            error("sentencia no valida: se esperaba identificador, if, for, read, write, return, '{', 'let' o ';'");
            avanzar();
            return "void";
        }
    }

    // Sentencia de asignacion o llamada a funcion
    private void SA() throws IOException {
        regla(35);
        Token tId = actual;
        String lexema = getLexema(tId);
        match("cod_id");
        
        if (es("cod_parIzq")) {
            // Es una llamada a funcion
            TablaSimbolos.Simbolo s = TablaSimbolos.buscar(lexema);
            if (s == null) {
                errorSemantico("funcion '" + lexema + "' no declarada");
            }
            match("cod_parIzq");
            AO(); // argumentos
            match("cod_parDer");
        } else {
            // Es una asignacion simple: variable = ... o variable %= ...
            TablaSimbolos.Simbolo s = TablaSimbolos.buscar(lexema);
            if (s == null || "-".equals(s.tipo)) {
                errorSemantico("variable '" + lexema + "' no declarada");
            }
            // regla(35); // Eliminado: duplicado
            
            String op = "";
            if (es("cod_asig")) {
                regla(36);
                match("cod_asig");
                op = "=";
            } else if (es("cod_asigRes")) {
                regla(37);
                match("cod_asigRes");
                op = "%=";
            } else {
                error("se esperaba '=' o '%='");
            }

            String tipoRhs = X(); // evaluamos la expresion de la derecha
            
            if (s != null && !"error".equals(tipoRhs)) {
                if ("=".equals(op)) {
                    if (!s.tipo.equals(tipoRhs)) {
                        errorSemantico("tipos incompatibles en asignacion: " + s.tipo + " = " + tipoRhs);
                    }
                } else if ("%=".equals(op)) {
                    if (!"entero".equals(s.tipo) || !"entero".equals(tipoRhs)) {
                        errorSemantico("operador %= requiere tipos enteros");
                    }
                }
            }
        }
        match("cod_pc");
    }

    // Bucle for (F0 ; F1 ; F2) S
    private void SF() throws IOException {
        regla(38);
        match("PR_for");
        match("cod_parIzq");
        
        F0(); // inicializacion
        match("cod_pc");
        F1(); // condicion
        match("cod_pc");
        F2(); // paso/incremento
        
        match("cod_parDer");
        B(); // cuerpo del for (tiene que ser un bloque entre llaves)
    }

    // Inicializacion del for
    private void F0() throws IOException {
        if (es("cod_id")) {
             // id = X
            regla(39);
            Token tId = actual;
            String lexema = getLexema(tId);
            match("cod_id");
            TablaSimbolos.Simbolo s = TablaSimbolos.buscar(lexema);
            if(s==null || "-".equals(s.tipo)) errorSemantico("variable en for no declarada");
            
            String op = "";
            if (es("cod_asig")) {
                regla(36);
                match("cod_asig");
                op = "=";
            } else if (es("cod_asigRes")) {
                regla(37);
                match("cod_asigRes");
                op = "%=";
            } else {
                error("se esperaba '=' o '%='");
            }

            String tX = X();
            if(s!=null && !"error".equals(tX)) {
               if(!s.tipo.equals(tX)) errorSemantico("tipos incompatibles en init for");
            }
        } else if (es("PR_let")) {
            // let T id = X
            regla(40);
            match("PR_let");
            zonaDeclaracion = true;
            String tipo = T();
            Token tId = actual;
            String lexema = getLexema(tId);
            match("cod_id");
            if(!lexema.isEmpty()) {
                int h = TablaSimbolos.gestionarId(lexema);
                TablaSimbolos.Simbolo s = TablaSimbolos.getSimbolo(h);
                if(s!=null) { s.tipo=tipo; s.desp=desp; desp+=getAncho(tipo); }
            }
            zonaDeclaracion = false;
            
            D1(tipo);
        } else if (es("cod_pc")) {
            regla(41); // parte vacia
        } else {
            error("inicializacion del for: se esperaba asignacion, 'let' o nada");
        }
    }

    // Condicion del for
    private void F1() throws IOException {
        if (esInicioExpr()) {
            regla(42);
            String t = X();
            if(!"error".equals(t) && !"boolean".equals(t)) errorSemantico("condicion for debe ser boolean");
        } else if (es("cod_pc")) {
            regla(43); // sin condicion
        } else {
            error("condicion del for: se esperaba una expresion o nada");
        }
    }

    // Incremento del for
    private void F2() throws IOException {
        if (es("cod_id")) {
            regla(44);
            Token tId = actual;
            String lexema = getLexema(tId);
            match("cod_id");
            TablaSimbolos.Simbolo s = TablaSimbolos.buscar(lexema);
            if(s==null || "-".equals(s.tipo)) errorSemantico("variable en incr for no declarada");
            
            String op = "";
            if (es("cod_asig")) {
                regla(36);
                match("cod_asig");
                op = "=";
            } else if (es("cod_asigRes")) {
                regla(37);
                match("cod_asigRes");
                op = "%=";
            } else {
                error("se esperaba '=' o '%='");
            }

            String tX = X();
            if(s!=null && !"error".equals(tX)) {
               if("=".equals(op) && !s.tipo.equals(tX)) errorSemantico("tipos incompatibles en incr for");
               if("%=".equals(op) && (!"entero".equals(s.tipo) || !"entero".equals(tX))) errorSemantico("%= requiere enteros");
            }
        } else if (es("cod_parDer")) {
            regla(45); // sin incremento
        } else {
            error("incremento del for: se esperaba asignacion o nada");
        }
    }

    // Sentencia if (X) S
    private void SI() throws IOException {
        regla(46);
        match("PR_if");
        match("cod_parIzq");
        String t = X();
        if(!"error".equals(t) && !"boolean".equals(t)) {
             errorSemantico("Condicion del if debe ser booleana");
        }
        match("cod_parDer");
        S();
    }

    // Sentencia read id ; (sin parentesis)
    private void SR() throws IOException {
        regla(47);
        match("PR_read");
        
        Token tId = actual;
        String lexema = getLexema(tId);
        match("cod_id");
        
        TablaSimbolos.Simbolo s = TablaSimbolos.buscar(lexema);
        if(s==null || "-".equals(s.tipo)) errorSemantico("Variable no declarada en read");
        else if("boolean".equals(s.tipo)) errorSemantico("No se puede hacer read de boolean");
        
        match("cod_pc");
    }

    // Sentencia write X ; (sin parentesis)
    private void SW() throws IOException {
        regla(48);
        match("PR_write");
        
        String t = X(); // expresion a escribir
        
        if("boolean".equals(t)) errorSemantico("No se puede hacer write de boolean");
        
        match("cod_pc");
    }

    // Sentencia return X
    private String ST() throws IOException {
        regla(49);
        match("PR_return");
        String tipo = X0();
        match("cod_pc");
        return tipo;
    }

    // Expresion opcional de retorno
    private String X0() throws IOException {
        if (esInicioExpr()) {
            regla(50);
            return X();
        } else if (es("cod_pc")) {
            regla(51); // return vacio
            return "void";
        } else {
            error("return incorrecto, se esperaba una expresion o ';'");
            return "error";
        }
    }

    // Una expresion (la mas general)
    private String X() throws IOException {
        regla(52);
        return X1();
    }

    // Nivel 1: Igualdades (==)
    private String X1() throws IOException {
        regla(53);
        String t = X2();
        return X12(t);
    }

    // Parte derecha de la igualdad
    private String X12(String inh) throws IOException {
        if (es("cod_rel")) { // ==
            regla(54);
            match("cod_rel");
            String t2 = X2();
            if (!"error".equals(inh) && !"error".equals(t2)) {
                 // la regla dice que ambos tienen que ser numericos
                 if (!esNumerico(inh) || !esNumerico(t2)) {
                     errorSemantico("== requiere numéricos");
                 }
            }
            return X12("boolean");
        } else {
            regla(55);
            return inh;
        }
    }

    // Nivel 2: Sumas (+)
    private String X2() throws IOException {
        regla(56);
        String t = X3();
        return X22(t);
    }

    // Parte derecha de la suma
    private String X22(String inh) throws IOException {
        if (es("cod_sum")) {
            regla(57);
            match("cod_sum");
            String t2 = X3();
            if (!"error".equals(inh) && !"error".equals(t2)) {
                if (inh.equals("entero") && t2.equals("entero")) {
                    // sigue siendo entero
                } else if ((inh.equals("entero") || inh.equals("real")) && (t2.equals("entero") || t2.equals("real"))) {
                    if (inh.equals("real") || t2.equals("real")) return X22("real");
                } else {
                    errorSemantico("operacion + no valida para tipos: " + inh + ", " + t2);
                }
            }
            return X22(inh);
        } else {
            regla(58);
            return inh;
        }
    }

    // Nivel 3: Negacion logica (!)
    private String X3() throws IOException {
        if (es("cod_log")) {
            regla(59);
            match("cod_log");
            String t = X3();
            if (!"error".equals(t) && !"boolean".equals(t)) {
                errorSemantico("operador ! requiere boolean");
            }
            return "boolean";
        } else if (esInicioPrimario()) {
            regla(60);
            return V();
        } else {
            error("se esperaba una expresion (identificador, numero, cadena o '(')");
            return "error";
        }
    }

    // Valores básicos (id, numero, cadena, parentesis)
    private String V() throws IOException {
        if (es("cod_id")) {
            regla(61);
            Token tId = actual;
            match("cod_id");
            return V2(tId);
        } else if (es("cod_ce")) {
            regla(62);
            match("cod_ce");
            return "entero";
        } else if (es("cod_cr")) {
            regla(63);
            match("cod_cr");
            return "real";
        } else if (es("cod_cad")) {
            regla(64);
            match("cod_cad");
            return "cadena";
        } else if (es("cod_parIzq")) {
            regla(65);
            match("cod_parIzq");
            String t = X();
            match("cod_parDer");
            return t;
        } else {
            error("se esperaba identificador, constante o '('");
            return "error";
        }
    }

    // Para distinguir entre variable normal o llamada a funcion
    private String V2(Token tId) throws IOException {
        String lexema = getLexema(tId);
        if (es("cod_parIzq")) {
            regla(66);
            match("cod_parIzq");
            AO();
            match("cod_parDer");
            TablaSimbolos.Simbolo s = TablaSimbolos.buscar(lexema);
            if (s == null || "-".equals(s.tipo)) {
                errorSemantico("funcion '" + lexema + "' no declarada");
                return "error";
            }
            return s.tipoRetorno != null ? s.tipoRetorno : s.tipo;
        } else {
            regla(67);
            TablaSimbolos.Simbolo s = TablaSimbolos.buscar(lexema);
            if (s == null || "-".equals(s.tipo)) {
                errorSemantico("variable '" + lexema + "' no declarada");
                return "error";
            }
            return s.tipo;
        }
    }

    // Argumentos opcionales de una funcion
    private void AO() throws IOException {
        if (esInicioExpr()) {
            regla(68);
            AL();
        } else if (es("cod_parDer")) {
            regla(69); // sin argumentos
        } else {
            error("argumento incorrecto en llamada a funcion");
        }
    }

    // Lista de argumentos
    private void AL() throws IOException {
        regla(70);
        X();
        AL2();
    }

    // Mas argumentos separados por coma
    private void AL2() throws IOException {
        if (es("cod_coma")) {
            regla(71);
            match("cod_coma");
            X();
            AL2();
        } else if (es("cod_parDer")) {
            regla(72); // final de la lista
        } else {
            error("se esperaba ',' o ')' en los argumentos");
        }
    }
}
