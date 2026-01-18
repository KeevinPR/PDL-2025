// Declaraciones, funcion con (void), read/write con y sin parentesis

let int miVariable = 10;
let string mensaje = "hola mundo";
let int numero;

// funcion sin parametros
function int miFuncion() {
    miVariable = miVariable + 5;
    return miVariable + numero;
}

// read sin parentesis
read numero;

// llamada a funcion como sentencia
miVariable = miFuncion();

// write con y sin parentesis
write miVariable;
write(mensaje);
