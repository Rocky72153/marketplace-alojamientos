import { access, readFile } from 'node:fs/promises';

const required = ['index.html', 'src/main.js', 'src/search.js', 'src/styles.css', 'tests/search.test.js'];
for (const path of required) await access(new URL('../' + path, import.meta.url));
const html = await readFile(new URL('../index.html', import.meta.url), 'utf8');
if (!html.includes('<html lang="es">')) throw new Error('index.html debe declarar el idioma español.');
if (!html.includes('id="search-form"')) throw new Error('Falta el formulario de búsqueda inicial.');
console.log('Verificación de archivos del frontend completada.');
