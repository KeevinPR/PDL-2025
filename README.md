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

## Ejemplo de salida

Si tienes este código:
```javascript
let variable1 = 42;
let variable2 = "hola";
```

La tabla de símbolos se guarda así:
```
TABLA PRINCIPAL # 1 :
* 'variable1'
+ Tipo : '-'
+ lineaPrimera : 1
+ nOcurrencias : 1

* 'variable2'
+ Tipo : '-'
+ lineaPrimera : 2
+ nOcurrencias : 1
```

## Palabras que NO se guardan

function, return, if, for, let, int, float, boolean, string, void, write, read, true, false

## Archivos del proyecto

- `SymbolTableManager.java` - La lógica interna
- `TSApi.java` - Lo que usa el analizador léxico
- `Main.java` - Demo para probar
