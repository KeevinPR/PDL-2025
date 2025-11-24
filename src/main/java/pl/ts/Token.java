package pl.ts;

// Token sencillo para el sintáctico
public class Token {
    public String codigo;   // PRlet, CODid, CODce, CODcad, CODsum, CODlog, CODpc, CODparIzq, CODLLizq, CODLLder, CODcoma, CODasig, CODrel, CODasigRes
    public String atributo; // lo que escribimos en el fichero de tokens
    public int linea;       // línea donde lo vio el AL

    public Token(String codigo, String atributo, int linea) {
        this.codigo = codigo;
        this.atributo = atributo;
        this.linea = linea;
    }
}