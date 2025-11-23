# myjs-ts-demo

Tabla de símbolos para análisis léxico en Java.

## Cómo compilar

```bash
mvn -q -DskipTests package
```

## Cómo usar

```bash
java -jar target/myjs-ts-demo.jar archivo.js salida.txt
```

## Para el Analizador Léxico

El analizador léxico debe usar estas 3 funciones:

```java
// Al empezar el análisis
TSApi.start("tabla_simbolos.txt");

// Cuando encuentra un identificador
int handle = TSApi.ensureId("nombreVariable", numeroLinea);

// Al terminar el análisis
TSApi.finish();
```

## Cómo funciona la conexión

1. El analizador léxico lee código JavaScript carácter por carácter
2. Cuando encuentra un identificador (como `variable1`), llama a `TSApi.ensureId()`
3. La tabla de símbolos guarda el identificador y devuelve un handle (número único)
4. El analizador léxico puede usar ese handle para referenciar el identificador

## Ejemplo de uso en el analizador

```java
// El analizador está en la línea 3 y encuentra "miVariable"
int handle = TSApi.ensureId("miVariable", 3);
// handle = 1 (primer identificador)

// Más tarde encuentra "miVariable" otra vez
int handle2 = TSApi.ensureId("miVariable", 7);
// handle2 = 1 (mismo identificador, mismo handle)
```

## Formato de salida

```
TABLA PRINCIPAL # 1 :
* 'variable1'
+ Tipo : '-'

* 'variable2'
+ Tipo : '-'
```

## Atributos

Atributos estándar: Tipo, Despl, numParam, TipoParamXX, ModoParamXX, TipoRetorno, EtiqFuncion, Param

```java
Symbol s = TSApi.buscar("variable1");
TSApi.setAtributo(s, "Despl", 4);
TSApi.setAtributo(s, "Tipo", "entero");
```

## Palabras reservadas

function, return, if, for, int, float, boolean, string, void, write, read

## Archivos del proyecto

- `SymbolTableManager.java` - La lógica interna
- `TSApi.java` - Lo que usa el analizador léxico
- `Main.java` - Demo para probar

# myjs-ts-demo

Proyecto de analizador léxico y tabla de símbolos para el lenguaje MyJS (PDL 2025, grupo 148).

## Compilar

```bash
mvn -q -DskipTests package
```

Esto genera el jar en `target/myjs-ts-demo.jar`.

## Ejecutar

```bash
java -jar target/myjs-ts-demo.jar fuente.javascript tokens.txt tabla_simbolos.txt
```

- `fuente.javascript`: programa MyJS de entrada.
- `tokens.txt`: fichero de salida con la lista de tokens.
- `tabla_simbolos.txt`: volcado de la tabla de símbolos global.

## Uso básico de la tabla de símbolos

El analizador léxico solo necesita tres llamadas a `TSApi`:

```java
// Al empezar el análisis
TSApi.start("tabla_simbolos.txt");

// Cuando reconoce un identificador
int handle = TSApi.ensureId(lexema, numeroLinea);
// Se usa ese handle como atributo del token CODid

// Al terminar el análisis
TSApi.finish();
```

La tabla de símbolos guarda cada identificador una sola vez y le asigna un número (handle). Ese número es el atributo que aparece en los tokens de tipo `CODid`.

## Formato de la tabla de símbolos

Ejemplo de salida de `tabla_simbolos.txt`:

```text
TABLA PRINCIPAL # 1 :
* 'miVariable'
+ Tipo : '-'

* 'otraVariable'
+ Tipo : '-'
```

Para esta primera entrega solo se usa el atributo `Tipo` con el valor `'-'`. En entregas siguientes se rellenarán más atributos (tipo real, desplazamiento, etc.).

## Archivos principales

- `Lexer.java`: analizador léxico de MyJS.
- `SymbolTableManager.java`: implementación de la tabla de símbolos.
- `TSApi.java`: interfaz sencilla entre el léxico y la tabla de símbolos.
- `Main.java`: punto de entrada, llama al léxico y cierra la TS.