import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// A static site, served from the root of its own domain. The Fenix Maven
// repository lives on GitHub Pages under a path; this does not, so no base.
export default defineConfig({
  plugins: [react()],
  build: { outDir: 'dist', sourcemap: false },
});
