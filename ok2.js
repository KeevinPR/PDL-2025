// Estructuras de control
// for, if simple, operador modulo-asignacion

let int contador;
let int suma;
let int resto;

suma = 0;
resto = 100;

for (contador = 1; contador == 10; contador = contador + 1) {
    suma = suma + contador;
    resto %= 3;
    
    if (resto == 0)
        write(contador);
}

write(suma);
