import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import path from "node:path";
import { defineConfig, type Plugin } from "vite";

/**
 * O shell visual da PWA não pode depender de uma única requisição de CSS no iOS.
 * Mantemos o arquivo CSS no bundle (para compatibilidade/cache), mas também colocamos
 * a folha principal dentro do index.html. Assim uma troca de versão nunca abre a Home
 * sem estilos enquanto os chunks das telas pesadas continuam sendo carregados sob demanda.
 */
function inlineEntryCss(): Plugin {
  return {
    name: "mic-rhema-inline-entry-css",
    enforce: "post",
    generateBundle(_options, bundle) {
      const html = bundle["index.html"];
      if (!html || html.type !== "asset" || typeof html.source !== "string") return;

      html.source = html.source.replace(
        /<link\s+rel="stylesheet"\s+crossorigin\s+href="([^"]+\/assets\/index-[^"]+\.css)">/,
        (tag, href: string) => {
          const marker = "/assets/";
          const markerIndex = href.lastIndexOf(marker);
          if (markerIndex < 0) return tag;
          const assetName = `assets/${href.slice(markerIndex + marker.length)}`;
          const css = bundle[assetName];
          if (!css || css.type !== "asset") return tag;
          return `<style data-mic-rhema-core>${String(css.source)}</style>`;
        },
      );
    },
  };
}

/** Build enxuto do GitHub Pages. Ferramentas Manus/JSX-location ficam somente no vite.config.ts de desenvolvimento. */
export default defineConfig({
  base: process.env.BASE_PATH || "/",
  plugins: [react(), tailwindcss(), inlineEntryCss()],
  resolve: {
    alias: {
      "@": path.resolve(import.meta.dirname, "client", "src"),
      "@shared": path.resolve(import.meta.dirname, "shared"),
      "@assets": path.resolve(import.meta.dirname, "attached_assets"),
    },
  },
  envDir: path.resolve(import.meta.dirname),
  root: path.resolve(import.meta.dirname, "client"),
  build: {
    outDir: path.resolve(import.meta.dirname, "dist/public"),
    emptyOutDir: true,
    target: "es2020",
  },
});
