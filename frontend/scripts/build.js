import { createHash } from 'node:crypto';
import { cp, mkdir, readFile, readdir, rm, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

async function sourceFiles(directory, prefix = '') {
  const files = [];
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const name = `${prefix}${entry.name}`;
    if (entry.isDirectory()) files.push(...await sourceFiles(new URL(`${entry.name}/`, directory), `${name}/`));
    else if (entry.isFile()) files.push(name);
  }
  return files.sort();
}

export async function buildFrontend(root = new URL('../', import.meta.url),
  dist = new URL('../dist/', import.meta.url), sameOrigin = process.env.MARKETPLACE_SAME_ORIGIN === 'true') {
  const source = new URL('src/', root);
  const hash = createHash('sha256');
  for (const file of await sourceFiles(source)) {
    const bytes = await readFile(new URL(file, source));
    hash.update(`${file}\0${bytes.length}\0`);
    hash.update(bytes);
  }
  const version = hash.digest('hex').slice(0, 16);
  let html = await readFile(new URL('index.html', root), 'utf8');
  if (sameOrigin) {
    const localApi = '<meta name="api-base-url" content="http://localhost:8080" />';
    if (!html.includes(localApi)) throw new Error('No se encontró la configuración local de la API en index.html.');
    html = html.replace(localApi, '<meta name="api-base-url" content="" />');
  }
  // A shared version keeps every relative module import and stylesheet in the same release.
  html = html.replaceAll('"/src/', `"/assets/${version}/`);
  await rm(dist, { recursive: true, force: true });
  await mkdir(new URL('assets/', dist), { recursive: true });
  await cp(source, new URL(`assets/${version}/`, dist), { recursive: true });
  await writeFile(new URL('index.html', dist), html);
  return { version };
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  await buildFrontend();
  console.log('Frontend estático preparado en frontend/dist.');
}
