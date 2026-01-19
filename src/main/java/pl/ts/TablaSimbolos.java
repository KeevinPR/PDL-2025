package pl.ts;

import java.io.*;
import java.util.*;

// Tabla de Simbolos simplificada
// Se encarga de guardar variables y funciones, y de manejar los ambitos
public class TablaSimbolos {
    
    // Clase interna para los simbolos (variables, funciones...)
    public static class Simbolo {
        public String lexema;
        public int id;          // identificador unico (handle)
        public String tipo;     // entero, real, boolean...
        public int desp;        // direccion relativa en memoria
        
        // Atributos extra para las funciones
        public int numParams;
        public String tipoRetorno;
        public String etiqFuncion;
        public int esParametro; // 1 si es parametro, 0 si no
        
        // Listas para los tipos de los parametros
        public String[] tipoParam = new String[10];
        public String[] modoParam = new String[10];

        public Simbolo(String lexema, int id) {
            this.lexema = lexema;
            this.id = id;
            this.tipo = "-";
            this.desp = 0;
            this.numParams = 0;
            this.tipoRetorno = null;
            this.etiqFuncion = null;
            this.esParametro = 0;
        }
    }

    // Clase interna para cada tabla (ambito)
    public static class Tabla {
        public int id;
        public String nombre;
        public List<Simbolo> simbolos;

        public Tabla(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
            this.simbolos = new ArrayList<>();
        }
    }

    // --- VARIABLES ESTATICAS (SINGLETON) ---
    private static PrintWriter escritor;
    private static List<Tabla> pilaTablas; 
    private static int contadorTablas;
    private static int contadorIds; 

    // Inicializar todo (se llama desde el Main)
    public static void inicializar(String ficheroSalida) throws IOException {
        escritor = new PrintWriter(new FileWriter(ficheroSalida, false));
        pilaTablas = new ArrayList<>();
        contadorTablas = 1;
        contadorIds = 1;
        
        // Creamos la tabla principal (Global)
        entrarBloque("TABLA PRINCIPAL");
        
        // Insertamos palabras reservadas o tipos basicos si hiciera falta
        Simbolo t = insertar("true"); t.tipo = "boolean";
        Simbolo f = insertar("false"); f.tipo = "boolean";
    }

    // Al terminar, cerramos ficheros
    public static void finalizar() {
        if (escritor != null) {
            // Cerramos las tablas que sigan abiertas (generalmente la global)
            while (!pilaTablas.isEmpty()) {
                salirBloque();
            }
            escritor.close();
        }
    }

    // CREAR NUEVO AMBITO
    public static void entrarBloque(String nombre) {
        Tabla nueva = new Tabla(contadorTablas++, nombre);
        pilaTablas.add(nueva);
    }

    // CERRAR AMBITO ACTUAL
    public static void salirBloque() {
        if (pilaTablas.isEmpty()) return;
        
        // Imprimimos la tabla antes de cerrarla
        Tabla actual = pilaTablas.get(pilaTablas.size() - 1);
        imprimirTabla(actual);
        
        pilaTablas.remove(pilaTablas.size() - 1);
    }

    // Obtener la tabla actual (para ver parametros, etc)
    public static Tabla getTablaActual() {
        if (pilaTablas.isEmpty()) return null;
        return pilaTablas.get(pilaTablas.size() - 1);
    }

    // --- OPERACIONES DE BUSQUEDA E INSERCION ---

    // Buscar en TODOS los ambitos (Global)
    public static Simbolo buscar(String lexema) {
        // Buscamos desde la ultima tabla (local) hacia la primera (global)
        for (int i = pilaTablas.size() - 1; i >= 0; i--) {
            Tabla t = pilaTablas.get(i);
            for (Simbolo s : t.simbolos) {
                if (s.lexema.equals(lexema)) {
                    return s;
                }
            }
        }
        return null;
    }

    // Buscar SOLO en el ambito actual (para no declarar repes)
    public static Simbolo buscarLocal(String lexema) {
        if (pilaTablas.isEmpty()) return null;
        Tabla actual = pilaTablas.get(pilaTablas.size() - 1);
        for (Simbolo s : actual.simbolos) {
            if (s.lexema.equals(lexema)) {
                return s;
            }
        }
        return null;
    }

    // Obtener un simbolo por su ID (usado por el Parser)
    public static Simbolo getSimbolo(int id) {
        for (Tabla t : pilaTablas) {
            for (Simbolo s : t.simbolos) {
                if (s.id == id) {
                    return s;
                }
            }
        }
        return null; // No deberia pasar
    }

    // Insertar un nuevo simbolo en la tabla actual
    public static Simbolo insertar(String lexema) {
        if (pilaTablas.isEmpty()) return null;
        Tabla actual = pilaTablas.get(pilaTablas.size() - 1);
        
        Simbolo nuevo = new Simbolo(lexema, contadorIds++);
        actual.simbolos.add(nuevo);
        return nuevo;
    }

    // Metodo especial para el Lexer: "Dame el ID de este lexema, si no existe crealo"
    public static int gestionarId(String lexema) {
        Simbolo s = buscarLocal(lexema);
        if (s == null) {
            s = insertar(lexema);
        }
        return s.id;
    }

    // --- IMPRESION (Formato especificado) ---
    private static void imprimirTabla(Tabla t) {
        escritor.println(t.nombre + " # " + t.id + " :");
        
        for (Simbolo s : t.simbolos) {
            escritor.println("* LEXEMA : '" + s.lexema + "'");
            escritor.println("  ATRIBUTOS :");
            escritor.println("  + tipo : '" + s.tipo + "'");
            escritor.println("  + despl : " + s.desp);
            
            if (s.numParams > 0) {
                escritor.println("  + numParam : " + s.numParams);
                for (int i = 0; i < s.numParams; i++) {
                    escritor.println("  + TipoParam" + (i + 1) + " : '" + 
                        (s.tipoParam[i] != null ? s.tipoParam[i] : "-") + "'");
                    if (s.modoParam[i] != null) {
                        escritor.println("  + ModoParam" + (i + 1) + " : " + s.modoParam[i]);
                    }
                }
            }
            if (s.tipoRetorno != null) {
                escritor.println("  + TipoRetorno : '" + s.tipoRetorno + "'");
            }
            if (s.etiqFuncion != null) {
                escritor.println("  + EtiqFuncion : '" + s.etiqFuncion + "'");
            }
            if (s.esParametro != 0) {
                escritor.println("  + param : " + s.esParametro);
            }
            escritor.println("--------- ----------");
        }
        escritor.println();
        escritor.flush();
    }
}
