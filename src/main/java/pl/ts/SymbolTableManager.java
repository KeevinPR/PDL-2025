package pl.ts;

import java.io.*;
import java.util.*;

public class SymbolTableManager {

    // Símbolo sencillo: lexema, handle y atributos
    public static class Symbol {
        public String lexema;
        public int handle;
        public String tipo;
        public Map<String, Object> atributos;

        public Symbol(String lexema, int handle) {
            this.lexema = lexema;
            this.handle = handle;
            this.tipo = "-";
            this.atributos = new LinkedHashMap<String, Object>();
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
            this.simbolos = new ArrayList<Symbol>();
            this.nextHandle = 1;
        }
    }

    private PrintWriter writer;
    private List<SymbolTable> tablas;
    private int nextTableId;

    public SymbolTableManager(String outputPath) throws IOException {
        this.writer = new PrintWriter(new FileWriter(outputPath, false));
        this.tablas = new ArrayList<SymbolTable>();
        this.nextTableId = 1;
        enterScope("TABLA PRINCIPAL");
    }

    public void enterScope(String titulo) {
        SymbolTable tabla = new SymbolTable(nextTableId++, titulo);
        tablas.add(tabla);
    }

    // Volcar y eliminar la tabla del tope
    public void exitScope() {
        if (tablas.isEmpty()) {
            return;
        }
        SymbolTable tabla = tablas.remove(tablas.size() - 1);
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

    public Symbol insertar(String lexema, int linea) {
        if (tablas.isEmpty()) {
            return null;
        }
        SymbolTable tablaActual = tablas.get(tablas.size() - 1);
        Symbol simbolo = new Symbol(lexema, tablaActual.nextHandle++);
        simbolo.atributos.put("lineaPrimera", linea);
        simbolo.atributos.put("nOcurrencias", 1);
        tablaActual.simbolos.add(simbolo);
        return simbolo;
    }

    public Symbol asegurar(String lexema, int linea) {
        Symbol simbolo = buscarAqui(lexema);
        if (simbolo == null) {
            simbolo = insertar(lexema, linea);
        } else {
            Object v = simbolo.atributos.get("nOcurrencias");
            int n = 0;
            if (v instanceof Integer) {
                n = (Integer) v;
            }
            simbolo.atributos.put("nOcurrencias", n + 1);
        }
        return simbolo;
    }

    public void setAtributo(Symbol simbolo, String nombre, Object valor) {
        simbolo.atributos.put(nombre, valor);
    }

    public void cerrarTodo() {
        while (!tablas.isEmpty()) {
            exitScope();
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