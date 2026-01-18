// Programa de ejemplo - Grupo 148
// Procesadores de Lenguajes - Universidad Politecnica de Madrid

// Declaraciones globales
let int numeroA;
let int numeroB;
let int resultado;
let float valorReal;
let string mensaje;
let int contador;
let int suma;
let int temp;

// Funcion que suma dos enteros
function int sumar(int a, int b) {
    temp = a + b;
    return temp;
}

// Funcion que verifica si un numero es par usando operador %=
function int esPar(int n) {
    temp = n;
    temp %= 2;
    
    if (temp == 0)
        return 1;
    
    return 0;
}

// Funcion que imprime un mensaje
function void mostrarMensaje(string msg) {
    write(msg);
}

// Funcion que calcula una suma acumulada
function int sumaHasta(int limite) {
    suma = 0;
    
    for (contador = 1; contador == limite; contador = contador + 1) {
        suma = suma + contador;
    }
    
    return suma;
}

// Funcion que trabaja con numeros reales
function float procesarReal(float x) {
    valorReal = x + 3.14;
    return valorReal;
}

// Funcion sin parametros que retorna void
function void inicializar() {
    numeroA = 0;
    numeroB = 0;
    resultado = 0;
}

// Funcion que usa negacion logica
function void procesarCondicion(int valor) {
    if (valor == 0)
        write("El valor es cero");
    
    if (!(valor == 0))
        write("El valor no es cero");
}

// PROGRAMA PRINCIPAL

// Inicializacion
inicializar();

// Mensajes y entrada de datos
mensaje = "Bienvenido al programa de ejemplo";
mostrarMensaje(mensaje);

write("Introduce el primer numero: ");
read numeroA;

write("Introduce el segundo numero: ");
read(numeroB);

// Operaciones aritmeticas
resultado = sumar(numeroA, numeroB);
write("La suma es: ");
write(resultado);

// Verificacion de paridad
write("Verificando si el primer numero es par...");
temp = esPar(numeroA);

if (temp == 1)
    write("El numero es par");

if (!(temp == 1))
    write("El numero es impar");

// Ejemplo con for y suma acumulada
write("Calculando suma de 1 a 10...");
resultado = sumaHasta(10);
write("Resultado: ");
write(resultado);

// Ejemplo con operador %=
write("Ejemplo con operador de resto-asignacion:");
temp = 100;
write("Valor inicial: ");
write(temp);

temp %= 7;
write("Resto de dividir entre 7: ");
write(temp);

// Ejemplo con numeros reales
write("Trabajando con numeros reales...");
valorReal = 2.71;
valorReal = procesarReal(valorReal);
write("Resultado: ");
write(valorReal);

// Ejemplo con for y acumulacion
write("Generando secuencia con for:");
suma = 0;

for (contador = 0; contador == 5; contador = contador + 1) {
    write(contador);
    suma = suma + contador;
}

write("Suma total: ");
write(suma);

// Ejemplo con condiciones
write("Probando condiciones logicas:");
procesarCondicion(numeroA);
procesarCondicion(0);

// Mensaje final
mensaje = "Programa finalizado correctamente";
mostrarMensaje(mensaje);
