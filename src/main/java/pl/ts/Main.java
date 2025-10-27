package pl.ts;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Main - Demo tabla de símbolos
 */
public class Main {
    
    private static final String[] PALABRAS_RESERVADAS = {
        "function", "return", "if", "for", 
        "int", "float", "boolean", "string", "void", 
        "write", "read"
    };
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Uso: java -jar myjs-ts-demo.jar archivo.js salida.txt");
            return;
        }
        
        String archivoFuente = args[0];
        String archivoSalida = args[1];
        
        try {
            TSApi.start(archivoSalida);
            String codigo = Files.readString(Paths.get(archivoFuente));
            String codigoLimpio = quitarComentarios(codigo);
            buscarIdentificadores(codigoLimpio);
            TSApi.finish();
            System.out.println("Listo! Guardado en: " + archivoSalida);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    private static String quitarComentarios(String codigo) {
        String[] lineas = codigo.split("\n");
        String resultado = "";
        
        for (String linea : lineas) {
            int pos = linea.indexOf("//");
            if (pos != -1) {
                linea = linea.substring(0, pos);
            }
            resultado += linea + "\n";
        }
        
        return resultado;
    }
    
    private static void buscarIdentificadores(String codigo) {
        String[] lineas = codigo.split("\n");
        
        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i];
            String[] palabras = linea.split(" ");
            
            for (String palabra : palabras) {
                palabra = palabra.replaceAll("[^A-Za-z0-9_]", "");
                
                if (esIdentificador(palabra) && !esReservada(palabra)) {
                    int handle = TSApi.ensureId(palabra, i + 1);
                    System.out.println("Encontrado: " + palabra + " (línea " + (i + 1) + ")");
                }
            }
        }
    }
    
    private static boolean esIdentificador(String palabra) {
        if (palabra.isEmpty()) return false;
        
        char primero = palabra.charAt(0);
        if (!Character.isLetter(primero) && primero != '_') {
            return false;
        }
        
        for (int i = 1; i < palabra.length(); i++) {
            char c = palabra.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }
        
        return true;
    }
    
    private static boolean esReservada(String palabra) {
        for (String reservada : PALABRAS_RESERVADAS) {
            if (reservada.equals(palabra)) {
                return true;
            }
        }
        return false;
    }
}
