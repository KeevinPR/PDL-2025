// ok3: for con asignación con resto (%=) y return de nuestro grupo 148

let int i;
let int resto;

function int pares(int max) {
    for (i = 0; i == max; i = i + 1) {
        resto = i;
        resto %= 2;          // operador específico de nuestro grupo 148
        if (resto == 0) {
            write (i);
        }
    }

    return 0;
}

resto = pares(2);