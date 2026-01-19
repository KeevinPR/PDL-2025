// Prueba error semantico (fuera de ambito)
function void test() {
    let int local = 5;
}
local = 10; // Error: variable local no visible aqui
