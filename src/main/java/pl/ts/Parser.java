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

        // si al terminar no estamos en EOF, algo raro ha pasado
        if (actual != null && !"EOF".equals(actual.codigo)) {
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
            case "CODpc": return " (falta ';' al final?)";
            case "CODparDer": return " (revisa los parentesis)";
            case "CODLLder": return " (falta '}' de cierre?)";
            case "CODLLizq": return " (falta '{' de apertura?)";
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
            case "CODpc": return "';'";
            case "CODparIzq": return "'('";
            case "CODparDer": return "')'";
            case "CODLLizq": return "'{'";
            case "CODLLder": return "'}'";
            case "CODid": return "identificador";
            case "CODcad": return "cadena";
            case "CODce": return "entero";
            case "CODcr": return "real";
            case "CODasig": return "'='";
            case "CODasigRes": return "'%='";
            case "CODcoma": return "','";
            case "CODsum": return "'+'";
            case "CODrel": return "'=='";
            case "CODlog": return "'!'";
            case "PRlet": return "'let'";
            case "PRfunction": return "'function'";
            case "PRint": return "'int'";
            case "PRfloat": return "'float'";
            case "PRboolean": return "'boolean'";
            case "PRstring": return "'string'";
            case "PRvoid": return "'void'";
            case "PRif": return "'if'";
            case "PRfor": return "'for'";
            case "PRreturn": return "'return'";
            case "PRwrite": return "'write'";
            case "PRread": return "'read'";
            case "EOF": return "fin de fichero";
            default: return codigo;
        }
    }

    // helpers para conjuntos FIRST/FOLLOW que usamos muchas veces

    private boolean esTipo() {
        return es("PRint") || es("PRfloat") || es("PRboolean") || es("PRstring");
    }

    private boolean esInicioSentencia() {
        return es("CODid") || es("PRfor") || es("PRif") || es("PRread")
                || es("PRwrite") || es("PRreturn") || es("CODLLizq")
                || es("CODpc");
    }

    private boolean esInicioExpr() {
        return es("CODlog") || es("CODparIzq") || es("CODcad")
                || es("CODce") || es("CODid") || es("CODcr");
    }

    private boolean esInicioPrimario() {
        return es("CODid") || es("CODce") || es("CODcr")
                || es("CODcad") || es("CODparIzq");
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
        match("EOF");
    }

    private void G() throws IOException {
        if (esInicioSentencia() || es("PRlet") || es("PRfunction")) {
            regla(2);
            E();
            G();
        } else if (es("EOF")) {
            regla(3); // lambda
        } else {
            error("se esperaba una declaracion, funcion o sentencia");
        }
    }

    private void E() throws IOException {
        if (es("PRlet")) {
            regla(4);
            D();
        } else if (es("PRfunction")) {
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
        if (!es("PRlet")) {
            error("se esperaba 'let' para declarar variable");
        }
        regla(7);
        match("PRlet");
        T();
        match("CODid");
        D1();
        match("CODpc");
    }

    private void D1() throws IOException {
        if (es("CODasig")) {
            regla(8);
            match("CODasig");
            X();
        } else if (es("CODpc")) {
            regla(9); // lambda
        } else {
            error("se esperaba '=' o ';' en la declaracion");
        }
    }

    private void T() throws IOException {
        if (es("PRint")) {
            regla(10);
            match("PRint");
        } else if (es("PRfloat")) {
            regla(11);
            match("PRfloat");
        } else if (es("PRboolean")) {
            regla(12);
            match("PRboolean");
        } else if (es("PRstring")) {
            regla(13);
            match("PRstring");
        } else {
            error("se esperaba un tipo: int, float, boolean o string");
        }
    }

    private void F() throws IOException {
        regla(14);
        match("PRfunction");
        R();
        match("CODid");
        match("CODparIzq");
        PO();
        match("CODparDer");
        B();
    }

    private void R() throws IOException {
        if (esTipo()) {
            regla(15);
            T();
        } else if (es("PRvoid")) {
            regla(16);
            match("PRvoid");
        } else {
            error("se esperaba un tipo (int, float, boolean, string) o 'void'");
        }
    }

    private void PO() throws IOException {
        if (esTipo()) {
            regla(17);
            PL();
        } else if (es("CODparDer")) {
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
        if (es("CODcoma")) {
            regla(20);
            match("CODcoma");
            PA();
            LP();
        } else if (es("CODparDer")) {
            regla(21); // lambda
        } else {
            error("se esperaba ',' o ')' en la lista de parametros");
        }
    }

    private void PA() throws IOException {
        regla(22);
        T();
        match("CODid");
    }

    private void B() throws IOException {
        regla(23);
        match("CODLLizq");
        LS();
        match("CODLLder");
    }

    private void LS() throws IOException {
        if (esInicioSentencia()) {
            regla(24);
            S();
            LS();
        } else if (es("CODLLder")) {
            regla(25); // lambda
        } else {
            error("se esperaba una sentencia o '}' para cerrar el bloque");
        }
    }

    private void S() throws IOException {
        if (es("CODid")) {
            regla(26);
            SA();
        } else if (es("PRfor")) {
            regla(27);
            SF();
        } else if (es("PRif")) {
            regla(28);
            SI();
        } else if (es("PRread")) {
            regla(29);
            SR();
        } else if (es("PRwrite")) {
            regla(30);
            SW();
        } else if (es("PRreturn")) {
            regla(31);
            ST();
        } else if (es("CODLLizq")) {
            regla(32);
            B();
        } else if (es("CODpc")) {
            regla(33);
            match("CODpc");
        } else {
            error("sentencia no valida: se esperaba identificador, if, for, read, write, return, '{' o ';'");
            avanzar();
        }
    }

    private void SA() throws IOException {
        regla(34);
        match("CODid");
        if (es("CODparIzq")) {
            // Llamada a función: id ( args ) ;
            match("CODparIzq");
            AO();
            match("CODparDer");
        } else {
            // Asignación: id OP X ;
            OP();
            X();
        }
        match("CODpc");
    }

    private void OP() throws IOException {
        if (es("CODasig")) {
            regla(35);
            match("CODasig");
        } else if (es("CODasigRes")) {
            regla(36);
            match("CODasigRes");
        } else {
            error("se esperaba '=' o '%='");
        }
    }

    private void SF() throws IOException {
        regla(37);
        match("PRfor");
        match("CODparIzq");
        F0();
        match("CODpc");
        F1();
        match("CODpc");
        F2();
        match("CODparDer");
        S();
    }

    private void F0() throws IOException {
        if (es("CODid")) {
            regla(38);
            match("CODid");
            OP();
            X();
        } else if (es("PRlet")) {
            regla(39);
            match("PRlet");
            T();
            match("CODid");
            D1();
        } else if (es("CODpc")) {
            regla(40); // lambda
        } else {
            error("inicializacion del for: se esperaba asignacion, 'let' o nada");
        }
    }

    private void F1() throws IOException {
        if (esInicioExpr()) {
            regla(41);
            X();
        } else if (es("CODpc")) {
            regla(42); // lambda
        } else {
            error("condicion del for: se esperaba una expresion o nada");
        }
    }

    private void F2() throws IOException {
        if (es("CODid")) {
            regla(43);
            match("CODid");
            OP();
            X();
        } else if (es("CODparDer")) {
            regla(44); // lambda
        } else {
            error("incremento del for: se esperaba asignacion o nada");
        }
    }

    // SS - Sentencia Simple (solo para cuerpo del if simple)
    // Solo permite: asignación/llamada, read, write, return, ;
    private void SS() throws IOException {
        if (es("CODid")) {
            regla(26);
            SA();
        } else if (es("PRread")) {
            regla(29);
            SR();
        } else if (es("PRwrite")) {
            regla(30);
            SW();
        } else if (es("PRreturn")) {
            regla(31);
            ST();
        } else if (es("CODpc")) {
            regla(33);
            match("CODpc");
        } else {
            error("el cuerpo del if solo admite sentencias simples (asignacion, read, write, return o ';')");
            avanzar();
        }
    }

    private void SI() throws IOException {
        regla(45);
        match("PRif");
        match("CODparIzq");
        X();
        match("CODparDer");
        SS();
    }

    private void SR() throws IOException {
        regla(46);
        match("PRread");
        if (es("CODparIzq")) {
            match("CODparIzq");
            match("CODid");
            match("CODparDer");
        } else {
            match("CODid");
        }
        match("CODpc");
    }

    private void SW() throws IOException {
        regla(47);
        match("PRwrite");
        if (es("CODparIzq")) {
            match("CODparIzq");
            X();
            match("CODparDer");
        } else if (es("CODpc")) {
            // write ; -> falta expresión
            error("write debe ir seguido de una expresion");
        } else {
            X();
        }
        match("CODpc");
    }

    private void ST() throws IOException {
        regla(48);
        match("PRreturn");
        X0();
        match("CODpc");
    }

    private void X0() throws IOException {
        if (esInicioExpr()) {
            regla(49);
            X();
        } else if (es("CODpc")) {
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
        if (es("CODrel")) { // ==
            regla(53);
            match("CODrel");
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
        if (es("CODsum")) {
            regla(56);
            match("CODsum");
            X3();
            X2p();
        } else {
            // Lambda: cualquier otro token termina la expresion aditiva
            // El error (si lo hay) se detectara en el nivel superior
            regla(57);
        }
    }

    private void X3() throws IOException {
        if (es("CODlog")) {
            regla(58);
            match("CODlog");
            X3();
        } else if (esInicioPrimario()) {
            regla(59);
            V();
        } else {
            error("se esperaba una expresion (identificador, numero, cadena o '(')");
        }
    }

    private void V() throws IOException {
        if (es("CODid")) {
            regla(60);
            match("CODid");
            Vp();
        } else if (es("CODce")) {
            regla(61);
            match("CODce");
        } else if (es("CODcr")) {
            regla(62);
            match("CODcr");
        } else if (es("CODcad")) {
            regla(63);
            match("CODcad");
        } else if (es("CODparIzq")) {
            regla(64);
            match("CODparIzq");
            X();
            match("CODparDer");
        } else {
            error("se esperaba identificador, constante o '('");
        }
    }

    private void Vp() throws IOException {
        if (es("CODparIzq")) {
            regla(65);
            match("CODparIzq");
            AO();
            match("CODparDer");
        } else {
            // Lambda: el identificador no es una llamada a funcion
            regla(66);
        }
    }

    private void AO() throws IOException {
        if (esInicioExpr()) {
            regla(67);
            AL();
        } else if (es("CODparDer")) {
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
        if (es("CODcoma")) {
            regla(70);
            match("CODcoma");
            X();
            ALp();
        } else if (es("CODparDer")) {
            regla(71); // lambda
        } else {
            error("se esperaba ',' o ')' en los argumentos");
        }
    }
}