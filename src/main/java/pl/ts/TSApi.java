package pl.ts;

import java.io.IOException;

public class TSApi {

    private static SymbolTableManager manager;

    public static void start(String outputFile) throws IOException {
        manager = new SymbolTableManager(outputFile);
    }

    public static void finish() {
        if (manager != null) {
            manager.cerrarTodo();
            manager = null;
        }
    }

    public static int ensureId(String lexema, int linea) {
        SymbolTableManager.Symbol simbolo = manager.asegurar(lexema, linea);
        return simbolo.handle;
    }

    public static void enterScope(String nombre) {
        manager.enterScope(nombre);
    }

    public static void exitScope() {
        manager.exitScope();
    }
}