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
            error("entrada extra al final del programa");
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
            error("se esperaba " + esperado + " y se ha encontrado " +
                  (actual == null ? "EOF" : actual.codigo));
            avanzar();
        }
    }

    // escribe número de regla en el fichero de parse
    private void regla(int n) throws IOException {
        parseOut.write(Integer.toString(n));
        parseOut.newLine();
    }

    private void error(String msg) throws IOException {
        int linea = (actual == null ? -1 : actual.linea);
        if (linea == -1) {
            errOut.write("Linea ? (SINTACTICO): " + msg);
        } else {
            errOut.write("Linea " + linea + " (SINTACTICO): " + msg);
        }
        errOut.newLine();
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
            error("se esperaba inicio de declaración, función o sentencia");
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
            error("se esperaba let, function o una sentencia");
        }
    }

    private void D() throws IOException {
        if (!es("PRlet")) {
            error("se esperaba let en una declaración");
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
            error("se esperaba '=' o ';' en la declaración");
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
            error("se esperaba un tipo (int,float,boolean,string)");
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
            error("se esperaba tipo de retorno o void");
        }
    }

    private void PO() throws IOException {
        if (esTipo()) {
            regla(17);
            PL();
        } else if (es("CODparDer")) {
            regla(18); // lambda
        } else {
            error("error en parámetros de la función");
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
            error("se esperaba ',' o ')' en la lista de parámetros");
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
            error("error en lista de sentencias");
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
            error("sentencia no válida");
            avanzar();
        }
    }

    private void SA() throws IOException {
        regla(34);
        match("CODid");
        OP();
        X();
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
            error("error en la inicialización del for");
        }
    }

    private void F1() throws IOException {
        if (esInicioExpr()) {
            regla(41);
            X();
        } else if (es("CODpc")) {
            regla(42); // lambda
        } else {
            error("error en la condición del for");
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
            error("error en el incremento del for");
        }
    }

    private void SI() throws IOException {
        regla(45);
        match("PRif");
        match("CODparIzq");
        X();
        match("CODparDer");
        S();
    }

    private void SR() throws IOException {
        regla(46);
        match("PRread");
        match("CODparIzq");
        match("CODid");
        match("CODparDer");
        match("CODpc");
    }

    private void SW() throws IOException {
        regla(47);
        match("PRwrite");
        match("CODparIzq");
        X();
        match("CODparDer");
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
            error("error en return");
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
        } else if (es("CODparDer") || es("CODcoma") || es("CODpc")) {
            regla(54); // lambda
        } else {
            error("error en expresión de igualdad");
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
        } else if (es("CODparDer") || es("CODcoma") || es("CODpc") || es("CODrel")) {
            regla(57); // lambda
        } else {
            error("error en expresión aditiva");
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
            error("error en expresión unaria");
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
        } else if (es("CODparDer") || es("CODsum") || es("CODcoma")
                || es("CODpc") || es("CODrel")) {
            regla(66); // lambda
        } else {
            error("error tras identificador");
        }
    }

    private void AO() throws IOException {
        if (esInicioExpr()) {
            regla(67);
            AL();
        } else if (es("CODparDer")) {
            regla(68); // lambda
        } else {
            error("error en lista de argumentos");
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
            error("error en lista de argumentos");
        }
    }
}