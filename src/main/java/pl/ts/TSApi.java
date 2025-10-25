package pl.ts;

import java.io.IOException;

/**
 * TSApi - Interfaz para usar la tabla de símbolos
 */
public class TSApi {
    
    private static SymbolTableManager manager;
    
    // Iniciar tabla de símbolos
    public static void start(String outputFile) throws IOException {
        manager = new SymbolTableManager(outputFile);
    }
    
    // Terminar y guardar
    public static void finish() {
        if (manager != null) {
            manager.cerrarTodo();
            manager = null;
        }
    }
    
    // Agregar identificador
    public static int ensureId(String lexema, int linea) {
        SymbolTableManager.Symbol simbolo = manager.asegurar(lexema, linea);
        return simbolo.handle;
    }
    
    // Entrar a nuevo ámbito
    public static void enterScope(String nombre) {
        manager.enterScope(nombre);
    }
    
    // Salir del ámbito actual
    public static void exitScope() {
        manager.exitScope();
    }
}
