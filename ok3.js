let int i;
let int resto;

function void pares(int max) {
    for (i = 0; i == max; i = i + 1) {
        resto = i;
        resto %= 2;     // operador específico de nuestro grupo
        if (resto == 0)
            write i;
    }
}

pares(10);