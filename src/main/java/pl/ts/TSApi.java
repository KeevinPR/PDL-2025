package pl.ts;

import java.io.IOException;

// Clase para que sea mas facil llamar a la Tabla de Simbolos desde cualquier sitio
public class TSApi {

    private static SymbolTableManager manager;
    
    public static SymbolTableManager getManager() {
        return manager;
    }

    // Crea el manager al principio
    public static void start(String outputFile) throws IOException {
        manager = new SymbolTableManager(outputFile);
    }

    // Vuelca todo al fichero final y limpia
    public static void finish() {
        if (manager != null) {
            manager.cerrarTodo();
            manager = null;
        }
    }

    // Busca un lexema y si no esta lo añade (para el lexer)
    public static int ensureId(String lexema, int linea) {
        SymbolTableManager.Symbol simbolo = manager.asegurar(lexema, linea);
        return simbolo.handle;
    }

    // Para abrir un nuevo ambito (cuando entramos en una funcion)
    public static void enterScope(String nombre) {
        manager.enterScope(nombre);
    }

    // Cerramos el ambito y volvemos al de antes
    public static void exitScope() {
        manager.exitScope();
    }

    // Coger los datos de un simbolo sabiendo su numero (handle)
    public static SymbolTableManager.Symbol getSymbol(int handle) {
        return manager.getSymbolByHandle(handle);
    }

    // Buscar una variable en todos los ambitos abiertos
    public static SymbolTableManager.Symbol buscar(String lexema) {
        return manager.buscarGlobal(lexema);
    }
}
