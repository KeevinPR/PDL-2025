package pl.ts;

// Clase para guardar la informacion de cada token
public class Token {
    public String codigo;   // Ej: cod_id, PR_let...
    public String atributo; // El valor o el puntero a la tabla
    public int linea;      // En que linea aparecio

    public Token(String codigo, String atributo, int linea) {
        this.codigo = codigo;
        this.atributo = atributo;
        this.linea = linea;
    }
}
