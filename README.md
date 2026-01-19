# PDL 2025 - Grupo 148
## Analizador MyJS (Léxico, Sintáctico y Semántico)

Compilador para el lenguaje MyJS desarrollado para la asignatura de Procesadores de Lenguajes.

## Ejecución

1. **Compilar**: `mvn package`
2. **Ejecutar**: `java -jar target/myjs-ts-demo.jar <fuente> <tokens> <ts>`

*Ejemplo:*
```bash
java -jar target\myjs-ts-demo-1.0.0.jar ok1.js tokens.txt tabla_simbolos.txt
```

## Salida
- **tokens.txt**: Listado de tokens generados.
- **ts.txt**: Volcado de la Tabla de Símbolos por ámbitos.
- **parse.txt**: Números de las reglas de derivación (LL1).
- **errores.txt**: Errores léxicos, sintácticos y semánticos.

## Estructura del Código

- **`Lexer.java`**: Escaneo de caracteres y creación de tokens. Gestiona errores léxicos y la inserción inicial en la TS.
- **`Parser.java`**: Análisis sintáctico descendente. Realiza también las **validaciones semánticas** (tipos, ámbitos, declaraciones duplicadas) durante el proceso.
- **`SymbolTableManager.java`**: Lógica de gestión de tablas de símbolos y control de ámbitos.
- **`Main.java`**: Punto de entrada que coordina el flujo completo.