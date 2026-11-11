import fs from 'fs';
import path from 'path';

const distDir = path.resolve('./dist');
const indexHtml = path.join(distDir, 'index.html');

if (fs.existsSync(indexHtml)) {
  const routes = [
    'owner/login',
    'owner/register',
    'vet/login',
    'vet/register',
    'login',
  ];

  for (const route of routes) {
    const routeDir = path.join(distDir, route);
    fs.mkdirSync(routeDir, { recursive: true });
    fs.copyFileSync(indexHtml, path.join(routeDir, 'index.html'));
  }
  console.log('✅ Generated static fallback HTML for routes:', routes);
}

