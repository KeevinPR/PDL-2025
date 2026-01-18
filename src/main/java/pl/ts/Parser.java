package pl.ts;

import java.io.*;
import java.util.List;

// analizador sintáctico LL(1) descendente 
// gramática y numeración de reglas 
public class Parser {

    private List<Token> tokens;
    private int pos;
    private Token actual;

    private BufferedWriter parseOut; // salida del parse
    private BufferedWriter errOut;   // se comparte con el léxico (errores.txt)
    
    // Control de errores: evita cascadas de errores en la misma linea
    private int ultimaLineaError = -1;
    private int erroresEnLinea = 0;
    private static final int MAX_ERRORES_POR_LINEA = 1;

    public Parser(List<Token> tokens, String rutaParse, String rutaErrores) throws IOException {
        this.tokens = tokens;
        this.pos = 0;
        if (tokens.size() > 0) {
            this.actual = tokens.get(0);
        } else {
            this.actual = null;
        }
        this.parseOut = new BufferedWriter(new FileWriter(rutaParse, false));
        // cabecera VASt,  D (descendente) o A (ascendente)
        parseOut.write("D");
        parseOut.newLine();

    
        // append = true para no machacar los errores léxicos
        this.errOut = new BufferedWriter(new FileWriter(rutaErrores, true));
    }

    public void analizar() throws IOException {
        if (actual == null) {
            cerrar();
            return;
        }

        P(); // símbolo inicial

        // si al terminar no estamos en cod_eof, algo raro ha pasado
        if (actual != null && !"cod_eof".equals(actual.codigo)) {
            error("sobra codigo despues del final del programa");
        }

        cerrar();
    }

    private void cerrar() throws IOException {
        parseOut.close();
        errOut.close();
    }

    //funciones auxiliares

    private boolean es(String codigo) {
        return actual != null && codigo.equals(actual.codigo);
    }

    private void avanzar() {
        if (pos < tokens.size() - 1) {
            pos++;
            actual = tokens.get(pos);
        } else {
            actual = tokens.get(tokens.size() - 1); //  EOF
        }
    }

    private void match(String esperado) throws IOException {
        if (es(esperado)) {
            avanzar();
        } else {
            error("se esperaba " + nombreLegible(esperado) + " y se encontro " +
                  nombreLegible(actual == null ? null : actual.codigo) + pista(esperado));
            avanzar();
        }
    }

    // pistas cortas para errores comunes
    private String pista(String esperado) {
        switch (esperado) {
            case "cod_pc": return " (falta ';' al final?)";
            case "cod_parDer": return " (revisa los parentesis)";
            case "cod_LLder": return " (falta '}' de cierre?)";
            case "cod_LLizq": return " (falta '{' de apertura?)";
            default: return "";
        }
    }

    // escribe número de regla en el fichero de parse
    private void regla(int n) throws IOException {
        parseOut.write(Integer.toString(n));
        parseOut.newLine();
    }

    private void error(String msg) throws IOException {
        int linea = (actual == null ? -1 : actual.linea);
        
        // Limitar errores por linea para evitar cascadas
        if (linea == ultimaLineaError) {
            erroresEnLinea++;
            if (erroresEnLinea > MAX_ERRORES_POR_LINEA) {
                return; // Ignorar errores adicionales en la misma linea
            }
        } else {
            ultimaLineaError = linea;
            erroresEnLinea = 1;
        }
        
        if (linea == -1) {
            errOut.write("Linea ? (SINTACTICO): " + msg);
        } else {
            errOut.write("Linea " + linea + " (SINTACTICO): " + msg);
        }
        errOut.newLine();
    }

    // traduce código interno a nombre legible para mensajes
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

    // helpers para conjuntos FIRST/FOLLOW que usamos muchas veces

    private boolean esTipo() {
        return es("PR_int") || es("PR_float") || es("PR_boolean") || es("PR_string");
    }

    private boolean esInicioSentencia() {
        return es("cod_id") || es("PR_for") || es("PR_if") || es("PR_read")
                || es("PR_write") || es("PR_return") || es("cod_LLizq")
                || es("cod_pc");
    }

    private boolean esInicioExpr() {
        return es("cod_log") || es("cod_parIzq") || es("cod_cad")
                || es("cod_ce") || es("cod_id") || es("cod_cr");
    }

    private boolean esInicioPrimario() {
        return es("cod_id") || es("cod_ce") || es("cod_cr")
                || es("cod_cad") || es("cod_parIzq");
    }

    //reglas de la gramática
    //numeración:
    //  1: P  -> G eof
    //  2: G  -> E G
    //  3: G  -> λ
    //  4: E  -> D
    //  5: E  -> F
    //  6: E  -> S
    //  7: D  -> let T id D1 ;
    //  8: D1 -> = X
    //  9: D1 -> λ
    // 10: T  -> int
    // 11: T  -> float
    // 12: T  -> boolean
    // 13: T  -> string
    // 14: F  -> function R id ( PO ) B
    // 15: R  -> T
    // 16: R  -> void
    // 17: PO -> PL
    // 18: PO -> λ
    // 19: PL -> PA LP
    // 20: LP -> , PA LP
    // 21: LP -> λ
    // 22: PA -> T id
    // 23: B  -> { LS }
    // 24: LS -> S LS
    // 25: LS -> λ
    // 26: S  -> SA
    // 27: S  -> SF
    // 28: S  -> SI
    // 29: S  -> SR
    // 30: S  -> SW
    // 31: S  -> ST
    // 32: S  -> B
    // 33: S  -> ;
    // 34: SA -> id OP X ;
    // 35: OP -> =
    // 36: OP -> %=
    // 37: SF -> for ( F0 ; F1 ; F2 ) S
    // 38: F0 -> id OP X
    // 39: F0 -> let T id D1
    // 40: F0 -> λ
    // 41: F1 -> X
    // 42: F1 -> λ
    // 43: F2 -> id OP X
    // 44: F2 -> λ
    // 45: SI -> if ( X ) S
    // 46: SR -> read ( id ) ;
    // 47: SW -> write ( X ) ;
    // 48: ST -> return X0 ;
    // 49: X0 -> X
    // 50: X0 -> λ
    // 51: X  -> X1
    // 52: X1 -> X2 X1'
    // 53: X1'-> == X2 X1'
    // 54: X1'-> λ
    // 55: X2 -> X3 X2'
    // 56: X2'-> + X3 X2'
    // 57: X2'-> λ
    // 58: X3 -> ! X3
    // 59: X3 -> V
    // 60: V  -> id V'
    // 61: V  -> entero
    // 62: V  -> real
    // 63: V  -> cadena
    // 64: V  -> ( X )
    // 65: V' -> ( AO )
    // 66: V' -> λ
    // 67: AO -> AL
    // 68: AO -> λ
    // 69: AL -> X AL'
    // 70: AL'-> , X AL'
    // 71: AL'-> λ

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
            regla(3); // lambda
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

    private void D() throws IOException {
        if (!es("PR_let")) {
            error("se esperaba 'let' para declarar variable");
        }
        regla(7);
        match("PR_let");
        T();
        match("cod_id");
        D1();
        match("cod_pc");
    }

    private void D1() throws IOException {
        if (es("cod_asig")) {
            regla(8);
            match("cod_asig");
            X();
        } else if (es("cod_pc")) {
            regla(9); // lambda
        } else {
            error("se esperaba '=' o ';' en la declaracion");
        }
    }

    private void T() throws IOException {
        if (es("PR_int")) {
            regla(10);
            match("PR_int");
        } else if (es("PR_float")) {
            regla(11);
            match("PR_float");
        } else if (es("PR_boolean")) {
            regla(12);
            match("PR_boolean");
        } else if (es("PR_string")) {
            regla(13);
            match("PR_string");
        } else {
            error("se esperaba un tipo: int, float, boolean o string");
        }
    }

    private void F() throws IOException {
        regla(14);
        match("PR_function");
        R();
        match("cod_id");
        match("cod_parIzq");
        PO();
        match("cod_parDer");
        B();
    }

    private void R() throws IOException {
        if (esTipo()) {
            regla(15);
            T();
        } else if (es("PR_void")) {
            regla(16);
            match("PR_void");
        } else {
            error("se esperaba un tipo (int, float, boolean, string) o 'void'");
        }
    }

    private void PO() throws IOException {
        if (esTipo()) {
            regla(17);
            PL();
        } else if (es("cod_parDer")) {
            regla(18); // lambda - ()
        } else {
            error("parametro incorrecto: se esperaba un tipo o ')' para cerrar");
        }
    }

    private void PL() throws IOException {
        regla(19);
        PA();
        LP();
    }

    private void LP() throws IOException {
        if (es("cod_coma")) {
            regla(20);
            match("cod_coma");
            PA();
            LP();
        } else if (es("cod_parDer")) {
            regla(21); // lambda
        } else {
            error("se esperaba ',' o ')' en la lista de parametros");
        }
    }

    private void PA() throws IOException {
        regla(22);
        T();
        match("cod_id");
    }

    private void B() throws IOException {
        regla(23);
        match("cod_LLizq");
        LS();
        match("cod_LLder");
    }

    private void LS() throws IOException {
        if (esInicioSentencia()) {
            regla(24);
            S();
            LS();
        } else if (es("cod_LLder")) {
            regla(25); // lambda
        } else {
            error("se esperaba una sentencia o '}' para cerrar el bloque");
        }
    }

    private void S() throws IOException {
        if (es("cod_id")) {
            regla(26);
            SA();
        } else if (es("PR_for")) {
            regla(27);
            SF();
        } else if (es("PR_if")) {
            regla(28);
            SI();
        } else if (es("PR_read")) {
            regla(29);
            SR();
        } else if (es("PR_write")) {
            regla(30);
            SW();
        } else if (es("PR_return")) {
            regla(31);
            ST();
        } else if (es("cod_LLizq")) {
            regla(32);
            B();
        } else if (es("cod_pc")) {
            regla(33);
            match("cod_pc");
        } else {
            error("sentencia no valida: se esperaba identificador, if, for, read, write, return, '{' o ';'");
            avanzar();
        }
    }

    private void SA() throws IOException {
        regla(34);
        match("cod_id");
        if (es("cod_parIzq")) {
            // Llamada a función: id ( args ) ;
            match("cod_parIzq");
            AO();
            match("cod_parDer");
        } else {
            regla(34); // SA -> id OP X ; (asignación)
            OP();
            X();
        }
        match("cod_pc");
    }

    private void OP() throws IOException {
        if (es("cod_asig")) {
            regla(35);
            match("cod_asig");
        } else if (es("cod_asigRes")) {
            regla(36);
            match("cod_asigRes");
        } else {
            error("se esperaba '=' o '%='");
        }
    }

    private void SF() throws IOException {
        regla(37);
        match("PR_for");
        match("cod_parIzq");
        F0();
        match("cod_pc");
        F1();
        match("cod_pc");
        F2();
        match("cod_parDer");
        S();
    }

    private void F0() throws IOException {
        if (es("cod_id")) {
            regla(38);
            match("cod_id");
            OP();
            X();
        } else if (es("PR_let")) {
            regla(39);
            match("PR_let");
            T();
            match("cod_id");
            D1();
        } else if (es("cod_pc")) {
            regla(40); // lambda
        } else {
            error("inicializacion del for: se esperaba asignacion, 'let' o nada");
        }
    }

    private void F1() throws IOException {
        if (esInicioExpr()) {
            regla(41);
            X();
        } else if (es("cod_pc")) {
            regla(42); // lambda
        } else {
            error("condicion del for: se esperaba una expresion o nada");
        }
    }

    private void F2() throws IOException {
        if (es("cod_id")) {
            regla(43);
            match("cod_id");
            OP();
            X();
        } else if (es("cod_parDer")) {
            regla(44); // lambda
        } else {
            error("incremento del for: se esperaba asignacion o nada");
        }
    }

    // SS - Sentencia Simple (solo para cuerpo del if simple)
    // Solo permite: asignación/llamada, read, write, return, ;
    private void SS() throws IOException {
        if (es("cod_id")) {
            regla(26);
            SA();
        } else if (es("PR_read")) {
            regla(29);
            SR();
        } else if (es("PR_write")) {
            regla(30);
            SW();
        } else if (es("PR_return")) {
            regla(31);
            ST();
        } else if (es("cod_pc")) {
            regla(33);
            match("cod_pc");
        } else {
            error("el cuerpo del if solo admite sentencias simples (asignacion, read, write, return o ';')");
            avanzar();
        }
    }

    private void SI() throws IOException {
        regla(45);
        match("PR_if");
        match("cod_parIzq");
        X();
        match("cod_parDer");
        SS();
    }

    private void SR() throws IOException {
        regla(46);
        match("PR_read");
        if (es("cod_parIzq")) {
            match("cod_parIzq");
            match("cod_id");
            match("cod_parDer");
        } else {
            match("cod_id");
        }
        match("cod_pc");
    }

    private void SW() throws IOException {
        regla(47);
        match("PR_write");
        if (es("cod_parIzq")) {
            match("cod_parIzq");
            X();
            match("cod_parDer");
        } else if (es("cod_pc")) {
            // write ; -> falta expresión
            error("write debe ir seguido de una expresion");
        } else {
            X();
        }
        match("cod_pc");
    }

    private void ST() throws IOException {
        regla(48);
        match("PR_return");
        X0();
        match("cod_pc");
    }

    private void X0() throws IOException {
        if (esInicioExpr()) {
            regla(49);
            X();
        } else if (es("cod_pc")) {
            regla(50); // lambda
        } else {
            error("return incorrecto, se esperaba una expresion o ';'");
        }
    }

    private void X() throws IOException {
        regla(51);
        X1();
    }

    private void X1() throws IOException {
        regla(52);
        X2();
        X1p();
    }

    private void X1p() throws IOException {
        if (es("cod_rel")) { // ==
            regla(53);
            match("cod_rel");
            X2();
            X1p();
        } else {
            // Lambda: cualquier otro token termina la expresion relacional
            // El error (si lo hay) se detectara en el nivel superior
            regla(54);
        }
    }

    private void X2() throws IOException {
        regla(55);
        X3();
        X2p();
    }

    private void X2p() throws IOException {
        if (es("cod_sum")) {
            regla(56);
            match("cod_sum");
            X3();
            X2p();
        } else {
            // Lambda: cualquier otro token termina la expresion aditiva
            // El error (si lo hay) se detectara en el nivel superior
            regla(57);
        }
    }

    private void X3() throws IOException {
        if (es("cod_log")) {
            regla(58);
            match("cod_log");
            X3();
        } else if (esInicioPrimario()) {
            regla(59);
            V();
        } else {
            error("se esperaba una expresion (identificador, numero, cadena o '(')");
        }
    }

    private void V() throws IOException {
        if (es("cod_id")) {
            regla(60);
            match("cod_id");
            Vp();
        } else if (es("cod_ce")) {
            regla(61);
            match("cod_ce");
        } else if (es("cod_cr")) {
            regla(62);
            match("cod_cr");
        } else if (es("cod_cad")) {
            regla(63);
            match("cod_cad");
        } else if (es("cod_parIzq")) {
            regla(64);
            match("cod_parIzq");
            X();
            match("cod_parDer");
        } else {
            error("se esperaba identificador, constante o '('");
        }
    }

    private void Vp() throws IOException {
        if (es("cod_parIzq")) {
            regla(65);
            match("cod_parIzq");
            AO();
            match("cod_parDer");
        } else {
            // Lambda: el identificador no es una llamada a funcion
            regla(66);
        }
    }

    private void AO() throws IOException {
        if (esInicioExpr()) {
            regla(67);
            AL();
        } else if (es("cod_parDer")) {
            regla(68); // lambda
        } else {
            error("argumento incorrecto en llamada a funcion");
        }
    }

    private void AL() throws IOException {
        regla(69);
        X();
        ALp();
    }

    private void ALp() throws IOException {
        if (es("cod_coma")) {
            regla(70);
            match("cod_coma");
            X();
            ALp();
        } else if (es("cod_parDer")) {
            regla(71); // lambda
        } else {
            error("se esperaba ',' o ')' en los argumentos");
        }
    }
}