// ok1: declaraciones globales y una función sencilla

let int miVariable = 10;
let string otraVariable = "hola";
let int numero = 42;

function int miFuncion() {
    // variableLocal es global implícita entera al usarse sin let
    variableLocal = miVariable + 5;
    return variableLocal + numero;
}

miVariable = miFuncion();
write (miVariable);