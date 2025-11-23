package pl.ts;

import java.io.*;
import java.util.*;

public class SymbolTableManager {
    // Entrada de la tabla de símbolos
    public static class Symbol {
        public String lexema;                 // nombre
        public int handle;                    // número interno de la TS
        public String tipo;                   // tipo MyJS, '-' mientras no se conozca
        public Map<String, Object> atributos; // atributos extra

        public Symbol(String lexema, int handle) {
            this.lexema = lexema;
            this.handle = handle;
            this.tipo = "-";
            this.atributos = new LinkedHashMap<>();
        }
    }

    // Tabla de símbolos de un ámbito
    public static class SymbolTable {
        public int id;
        public String titulo;
        public List<Symbol> simbolos;
        public int nextHandle;

        public SymbolTable(int id, String titulo) {
            this.id = id;
            this.titulo = titulo;
            this.simbolos = new ArrayList<>();
            this.nextHandle = 1;
        }
    }

    private PrintWriter writer;
    private List<SymbolTable> tablas;
    private int nextTableId;

    public SymbolTableManager(String outputPath) throws IOException {
        // false -> sobrescribe siempre el fichero de TS
        this.writer = new PrintWriter(new FileWriter(outputPath, false));
        this.tablas = new ArrayList<>();
        this.nextTableId = 1;
        // En esta entrega solo usamos la tabla global
        enterScope("TABLA PRINCIPAL");
    }

    // Crea una nueva tabla (nuevo ámbito)
    public void enterScope(String titulo) {
        SymbolTable tabla = new SymbolTable(nextTableId++, titulo);
        tablas.add(tabla);
    }

    // Vuelca la tabla actual al fichero
    public void exitScope() {
        if (tablas.isEmpty()) {
            return;
        }
        SymbolTable tabla = tablas.get(tablas.size() - 1);
        escribirTabla(tabla);
        writer.flush();
    }

    // Busca un lexema en la tabla actual
    public Symbol buscarAqui(String lexema) {
        if (tablas.isEmpty()) {
            return null;
        }
        SymbolTable tablaActual = tablas.get(tablas.size() - 1);
        for (Symbol simbolo : tablaActual.simbolos) {
            if (simbolo.lexema.equals(lexema)) {
                return simbolo;
            }
        }
        return null;
    }

    // Inserta un nuevo símbolo en la tabla actual
    public Symbol insertar(String lexema) {
        if (tablas.isEmpty()) {
            return null;
        }
        SymbolTable tablaActual = tablas.get(tablas.size() - 1);
        Symbol simbolo = new Symbol(lexema, tablaActual.nextHandle++);
        tablaActual.simbolos.add(simbolo);
        return simbolo;
    }

    // Devuelve el símbolo, creándolo si no existe
    public Symbol asegurar(String lexema, int linea) {
        Symbol simbolo = buscarAqui(lexema);
        if (simbolo == null) {
            simbolo = insertar(lexema);
        }
        // 'linea' se usará en fases posteriores (ahora no se guarda)
        return simbolo;
    }

    // Para fases posteriores, cuando el semántico rellene más información
    public void setAtributo(Symbol simbolo, String nombre, Object valor) {
        simbolo.atributos.put(nombre, valor);
    }

    // Cierra todas las tablas y las vuelca al fichero
    public void cerrarTodo() {
        for (int i = tablas.size() - 1; i >= 0; i--) {
            escribirTabla(tablas.get(i));
        }
        writer.close();
    }

    // Volcado en el formato especificado en la práctica
    private void escribirTabla(SymbolTable tabla) {
        writer.println(tabla.titulo + " # " + tabla.id + " :");
        for (Symbol simbolo : tabla.simbolos) {
            writer.println("* '" + simbolo.lexema + "'");
            writer.println("+ Tipo : '" + simbolo.tipo + "'");
            // Ningun atributo extra por ahora
            writer.println();
        }
    }
}