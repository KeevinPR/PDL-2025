package pl.ts;

import java.io.*;
import java.util.*;

// Clase que maneja todas las tablas de simbolos y los ambitos
public class SymbolTableManager {
    
    // Lo que guardamos de cada variable o funcion
    public static class Symbol {
        public String lexema;
        public int handle;      // un numero unico para identificarlo
        public String tipo;     // int, float, boolean, string...
        public int desp;        // el desplazamiento en memoria
        
        // Datos para las funciones
        public int numParams;
        public String tipoRetorno;
        public String etiqFuncion;
        public int param; // 1 si es un parametro de funcion, 0 si no
        
        // Para los parametros (maximo 10 para no complicar)
        public String[] tipoParam = new String[10];
        public String[] modoParam = new String[10];

        public Symbol(String lexema, int handle) {
            this.lexema = lexema;
            this.handle = handle;
            this.tipo = "-";
            this.desp = 0;
            this.numParams = 0;
            this.tipoRetorno = null;
            this.etiqFuncion = null;
            this.param = 0;
        }
    }

    // Una tabla de simbolos para un ambito concreto (global o de una funcion)
    public static class SymbolTable {
        public int id;
        public String titulo;
        public List<Symbol> simbolos;
        public SymbolTable(int id, String titulo) {
            this.id = id;
            this.titulo = titulo;
            this.simbolos = new ArrayList<>();
        }
    }

    private PrintWriter writer;
    private List<SymbolTable> tablas; // Pila de tablas para los ambitos
    private int nextTableId;
    private int nextHandle; 

    public SymbolTableManager(String outputPath) throws IOException {
        // Inicializamos el fichero de salida
        this.writer = new PrintWriter(new FileWriter(outputPath, false));
        this.tablas = new ArrayList<>();
        this.nextTableId = 1;
        this.nextHandle = 1;
        
        // Creamos la tabla global
        enterScope("TABLA PRINCIPAL");
        
        // Metemos true y false por defecto
        Symbol t = insertar("true"); t.tipo = "boolean"; t.desp = 0;
        Symbol f = insertar("false"); f.tipo = "boolean"; f.desp = 0;
    }

    // Cuando entramos en una funcion, creamos una tabla nueva
    public void enterScope(String titulo) {
        SymbolTable tabla = new SymbolTable(nextTableId++, titulo);
        tablas.add(tabla);
    }

    // Coger la tabla que estamos usando ahora (la ultima de la lista)
    public SymbolTable getTablaActual() {
        if (tablas.isEmpty()) return null;
        return tablas.get(tablas.size() - 1);
    }

    // Cuando salimos de una funcion, imprimimos su tabla y la borramos de la pila
    public void exitScope() {
        if (tablas.isEmpty()) {
            return;
        }
        SymbolTable tabla = tablas.get(tablas.size() - 1);
        escribirTabla(tabla);
        writer.flush();
        tablas.remove(tablas.size() - 1); 
    }

    // Buscar si una variable ya existe en el ambito actual
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

    // Buscar una variable en el ambito actual y si no, en los padres (recursivo hacia arriba)
    public Symbol buscarGlobal(String lexema) {
        for (int i = tablas.size() - 1; i >= 0; i--) {
            SymbolTable tabla = tablas.get(i);
            for (Symbol s : tabla.simbolos) {
                if (s.lexema.equals(lexema)) {
                    return s;
                }
            }
        }
        return null;
    }

    // Buscar un simbolo por su numero identificador
    public Symbol getSymbolByHandle(int handle) {
        for (SymbolTable tabla : tablas) {
            for (Symbol s : tabla.simbolos) {
                if (s.handle == handle) {
                    return s;
                }
            }
        }
        return null;
    }

    // Meter una variable nueva en la tabla de ahora
    public Symbol insertar(String lexema) {
        if (tablas.isEmpty()) {
            return null;
        }
        SymbolTable tablaActual = tablas.get(tablas.size() - 1);
        Symbol simbolo = new Symbol(lexema, nextHandle++);
        tablaActual.simbolos.add(simbolo);
        return simbolo;
    }

    // Si existe la devuelve, si no la crea (util para el lexer)
    public Symbol asegurar(String lexema, int linea) {
        Symbol simbolo = buscarAqui(lexema);
        if (simbolo == null) {
            simbolo = insertar(lexema);
        }
        return simbolo;
    }

    // Al terminar el programa, volcamos las tablas que queden abiertas
    public void cerrarTodo() {
        for (int i = tablas.size() - 1; i >= 0; i--) {
            escribirTabla(tablas.get(i));
        }
        writer.close();
    }

    // Funcion para imprimir la tabla en el formato que nos han pedido
    private void escribirTabla(SymbolTable tabla) {
        writer.println(tabla.titulo + " # " + tabla.id + " :");
        
        for (Symbol simbolo : tabla.simbolos) {
            writer.println("* LEXEMA : '" + simbolo.lexema + "'");
            
            writer.println("  ATRIBUTOS :"); 
            
            writer.println("  + tipo : '" + simbolo.tipo + "'");
            writer.println("  + despl : " + simbolo.desp);
            
            if (simbolo.numParams > 0) {
                writer.println("  + numParam : " + simbolo.numParams);
                for(int i=0; i<simbolo.numParams; i++) {
                     writer.println("  + TipoParam" + (i+1) + " : '" + 
                         (simbolo.tipoParam[i]!=null ? simbolo.tipoParam[i] : "-") + "'");
                     if (simbolo.modoParam[i] != null) {
                         writer.println("  + ModoParam" + (i+1) + " : " + simbolo.modoParam[i]);
                     }
                }
            }
            if (simbolo.tipoRetorno != null) {
                 writer.println("  + TipoRetorno : '" + simbolo.tipoRetorno + "'");
            }
            if (simbolo.etiqFuncion != null) {
                 writer.println("  + EtiqFuncion : '" + simbolo.etiqFuncion + "'");
            }
            if (simbolo.param != 0) {
                 writer.println("  + param : " + simbolo.param);
            }

            writer.println("--------- ----------");
        }
        writer.println(); 
    }
}
