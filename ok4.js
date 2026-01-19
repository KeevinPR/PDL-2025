// Prueba funciones y ambitos (scope)
let int g = 100;
function int suma(int a, int b) {
    return a + b;
}
let int res = 0;
res = suma(g, 50);
write(res);
