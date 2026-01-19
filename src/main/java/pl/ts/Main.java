package pl.ts;

import java.util.List;

// Clase principal que arranca el compilador
public class Main {

    public static void main(String[] args) {
        // Miramos que nos pasen los 3 archivos obligatorios
        if (args.length != 3) {
            System.err.println("Uso: java -cp target/classes pl.ts.Main fichero.js tokens.txt tabla_simbolos.txt");
            return;
        }

        // Nombres de los ficheros que nos dan por argumentos
        String archivoFuente = args[0];
        String archivoTokens = args[1];
        String archivoTS     = args[2];

        // Estos son fijos segun nos han pedido
        String archivoParse  = "parse.txt";
        String archivoErrores = "errores.txt";

        try {
            // Inicializar la tabla de simbolos
            TablaSimbolos.inicializar(archivoTS);
            
            // Primero el Lexer para sacar los tokens
            AnalizadorLexico lexer = new AnalizadorLexico(archivoFuente, archivoTokens, archivoErrores);
            lexer.analizar();

            // Despues el Parser usando la lista de tokens del lexer
            List<Token> listaTokens = lexer.getTokens();
            AnalizadorSintactico parser = new AnalizadorSintactico(listaTokens, archivoParse, archivoErrores);
            parser.analizar();

            // Cerramos todo
            TablaSimbolos.finalizar();

            System.out.println("Analisis completado.");
            System.out.println("Tokens en: " + archivoTokens);
            System.out.println("Tabla de simbolos en: " + archivoTS);
            System.out.println("Parse en: " + archivoParse);
            System.out.println("Errores (si los hay) en: " + archivoErrores);

        } catch (Exception e) {
            System.err.println("Error durante el analisis: " + e.getMessage());
        }
    }
}
