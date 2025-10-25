package pl.ts;

import java.io.*;
import java.util.*;

/**
 * SymbolTableManager - Maneja tablas de símbolos
 */
public class SymbolTableManager {
    
    // Clase símbolo
    public static class Symbol {
        public String lexema;
        public int handle;
        public String tipo;
        public Map<String, Object> atributos;
        
        public Symbol(String lexema, int handle) {
            this.lexema = lexema;
            this.handle = handle;
            this.tipo = "-";
            this.atributos = new LinkedHashMap<>();
        }
    }
    
    // Tabla de símbolos
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
        this.writer = new PrintWriter(new FileWriter(outputPath, false));
        this.tablas = new ArrayList<>();
        this.nextTableId = 1;
        enterScope("TABLA PRINCIPAL");
    }
    
    public void enterScope(String titulo) {
        SymbolTable tabla = new SymbolTable(nextTableId++, titulo);
        tablas.add(tabla);
    }
    
    public void exitScope() {
        if (tablas.isEmpty()) {
            return;
        }
        SymbolTable tabla = tablas.get(tablas.size() - 1);
        escribirTabla(tabla);
        writer.flush();
    }
    
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
    
    public Symbol insertar(String lexema) {
        if (tablas.isEmpty()) {
            return null;
        }
        SymbolTable tablaActual = tablas.get(tablas.size() - 1);
        Symbol simbolo = new Symbol(lexema, tablaActual.nextHandle++);
        tablaActual.simbolos.add(simbolo);
        return simbolo;
    }
    
    public Symbol asegurar(String lexema, int linea) {
        Symbol simbolo = buscarAqui(lexema);
        if (simbolo == null) {
            simbolo = insertar(lexema);
        }
        return simbolo;
    }
    
    public void setAtributo(Symbol simbolo, String nombre, Object valor) {
        simbolo.atributos.put(nombre, valor);
    }
    
    public void cerrarTodo() {
        for (int i = tablas.size() - 1; i >= 0; i--) {
            escribirTabla(tablas.get(i));
        }
        writer.close();
    }
    
    private void escribirTabla(SymbolTable tabla) {
        writer.println(tabla.titulo + " # " + tabla.id + " :");
        
        for (Symbol simbolo : tabla.simbolos) {
            writer.println("* '" + simbolo.lexema + "'");
            writer.println("+ Tipo : '" + simbolo.tipo + "'");
            
            for (Map.Entry<String, Object> entrada : simbolo.atributos.entrySet()) {
                String nombre = entrada.getKey();
                Object valor = entrada.getValue();
                
                writer.print("+ " + nombre + " : ");
                if (valor instanceof String) {
                    writer.println("'" + valor + "'");
                } else {
                    writer.println(valor);
                }
            }
            writer.println();
        }
    }
}
