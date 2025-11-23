package pl.ts;

public class Main {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Uso: java -cp target/classes pl.ts.Main fichero.js tokens.txt tabla_simbolos.txt");
            return;
        }

        String archivoFuente = args[0];
        String archivoTokens = args[1];
        String archivoTS = args[2];

        try {
            TSApi.start(archivoTS);

            Lexer lexer = new Lexer(archivoFuente, archivoTokens);
            lexer.analizar();

            TSApi.finish();

            System.out.println("Analisis completado.");
            System.out.println("Tokens en: " + archivoTokens);
            System.out.println("Tabla de simbolos en: " + archivoTS);
        } catch (Exception e) {
            System.err.println("Error durante el analisis: " + e.getMessage());
        }
    }
}