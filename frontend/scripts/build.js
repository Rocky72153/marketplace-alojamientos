import { cp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
const root = new URL('../', import.meta.url);
const dist = new URL('../dist/', import.meta.url);
await rm(dist, { recursive: true, force: true });
await mkdir(dist, { recursive: true });
for (const file of ['index.html', 'src']) await cp(new URL(file, root), new URL(file, dist), { recursive: true });
if (process.env.MARKETPLACE_SAME_ORIGIN === 'true') {
  const index = new URL('index.html', dist);
  const html = await readFile(index, 'utf8');
  const localApi = '<meta name="api-base-url" content="http://localhost:8080" />';
  if (!html.includes(localApi)) throw new Error('No se encontró la configuración local de la API en index.html.');
  await writeFile(index, html.replace(localApi, '<meta name="api-base-url" content="" />'));
}
console.log('Frontend estático preparado en frontend/dist.');
