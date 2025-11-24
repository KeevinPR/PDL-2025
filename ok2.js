// ok2: función con parámetro, if y for

let int contador;
let int i;

function int contarHasta(int limite) {
    contador = 0;

    for (i = 0; i == limite; i = i + 1) {
        if (contador == 0) {
            contador = contador + 1;
        }
    }

    return contador;
}

let int resultado;
resultado = contarHasta(0);
write (resultado);