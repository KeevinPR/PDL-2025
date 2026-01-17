package pl.ts;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Uso: java -cp target/classes pl.ts.Main fichero.js tokens.txt tabla_simbolos.txt");
            return;
        }

        String archivoFuente = args[0];
        String archivoTokens = args[1];
        String archivoTS     = args[2];

        String archivoParse  = "parse.txt";
        String archivoErrores = "errores.txt";

        try {
            // Tabla de símbolos
            TSApi.start(archivoTS);

            // Analizador léxico
            Lexer lexer = new Lexer(archivoFuente, archivoTokens, archivoErrores);
            lexer.analizar();

            TSApi.finish();

            // Analizador sintáctico
            List<Token> listaTokens = lexer.getTokens();
            Parser parser = new Parser(listaTokens, archivoParse, archivoErrores);
            parser.analizar();

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