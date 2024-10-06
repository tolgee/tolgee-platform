// vite.config.ts
import { defineConfig, loadEnv } from "file:///Users/huglx/tolgee/tolgee-platform/webapp/node_modules/vite/dist/node/index.js";
import react from "file:///Users/huglx/tolgee/tolgee-platform/webapp/node_modules/@vitejs/plugin-react/dist/index.mjs";
import viteTsconfigPaths from "file:///Users/huglx/tolgee/tolgee-platform/webapp/node_modules/vite-tsconfig-paths/dist/index.mjs";
import svgr from "file:///Users/huglx/tolgee/tolgee-platform/webapp/node_modules/vite-plugin-svgr/dist/index.js";
import { nodePolyfills } from "file:///Users/huglx/tolgee/tolgee-platform/webapp/node_modules/vite-plugin-node-polyfills/dist/index.js";
import mdx from "file:///Users/huglx/tolgee/tolgee-platform/webapp/node_modules/@mdx-js/rollup/index.js";
import { viteStaticCopy } from "file:///Users/huglx/tolgee/tolgee-platform/webapp/node_modules/vite-plugin-static-copy/dist/index.js";
import { resolve as resolve2 } from "path";

// dataCy.plugin.ts
import { readdir, readFile, writeFile } from "fs/promises";
import { resolve } from "path";
import { existsSync } from "fs";
var SRC_PATH = resolve("./src");
function extractDataCy() {
  const fileItems = {};
  async function generate(files) {
    await processFiles(files);
    const sortedItems = getSortedItems();
    const fileContent = await generateFileContent(sortedItems);
    await writeToFile(fileContent);
  }
  async function processFiles(files) {
    for (const file of files) {
      await processFile(file);
    }
  }
  async function writeToFile(fileContent) {
    await writeFile(
      resolve(`../e2e/cypress/support/dataCyType.d.ts`),
      fileContent
    );
  }
  async function generateFileContent(sortedItems) {
    let fileContent = "declare namespace DataCy {\n";
    fileContent += "    export type Value = \n        " + sortedItems.map((i) => `"${i}"`).join(" |\n        ") + "\n}";
    return fileContent;
  }
  function getSortedItems() {
    const items = Object.values(fileItems).reduce(
      (acc, curr) => [...acc, ...curr],
      []
    );
    const itemsSet = new Set(items);
    return [...itemsSet].sort();
  }
  async function processFile(file) {
    if (/.*\.tsx?$/.test(file)) {
      if (!existsSync(file)) {
        fileItems[file] = [];
        return;
      }
      const content = (await readFile(file)).toString();
      const matches = content.matchAll(
        /["']?data-?[c|C]y["']?\s*[=:]\s*{?["'`]([A-Za-z0-9-_\s]+)["'`]?}?/g
      );
      fileItems[file] = [];
      for (const match of matches) {
        fileItems[file].push(match[1]);
      }
    }
  }
  async function getFiles(dir) {
    const dirents = await readdir(dir, { withFileTypes: true });
    const files = await Promise.all(
      dirents.map((dirent) => {
        const res = resolve(dir, dirent.name);
        return dirent.isDirectory() ? getFiles(res) : res;
      })
    );
    return Array.prototype.concat(...files);
  }
  return {
    name: "extract-data-cy",
    async buildStart() {
      const files = await getFiles(SRC_PATH);
      await generate(files);
    },
    async watchChange(id) {
      generate([id]);
    }
  };
}

// vite.config.ts
import rehypeHighlight from "file:///Users/huglx/tolgee/tolgee-platform/webapp/node_modules/rehype-highlight/index.js";
var vite_config_default = defineConfig(({ mode }) => {
  process.env = { ...process.env, ...loadEnv(mode, process.cwd()) };
  return {
    // depending on your application, base can also be "/"
    base: "/",
    plugins: [
      react(),
      viteTsconfigPaths(),
      svgr(),
      mdx({ rehypePlugins: [rehypeHighlight] }),
      nodePolyfills(),
      extractDataCy(),
      viteStaticCopy({
        targets: [
          {
            src: resolve2("node_modules/@tginternal/language-util/flags"),
            dest: ""
          }
        ]
      })
    ],
    server: {
      // this ensures that the browser opens upon server start
      open: true,
      // this sets a default port to 3000
      port: Number(process.env.VITE_PORT) || 3e3
    }
  };
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcudHMiLCAiZGF0YUN5LnBsdWdpbi50cyJdLAogICJzb3VyY2VzQ29udGVudCI6IFsiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIi9Vc2Vycy9odWdseC90b2xnZWUvdG9sZ2VlLXBsYXRmb3JtL3dlYmFwcFwiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiL1VzZXJzL2h1Z2x4L3RvbGdlZS90b2xnZWUtcGxhdGZvcm0vd2ViYXBwL3ZpdGUuY29uZmlnLnRzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ltcG9ydF9tZXRhX3VybCA9IFwiZmlsZTovLy9Vc2Vycy9odWdseC90b2xnZWUvdG9sZ2VlLXBsYXRmb3JtL3dlYmFwcC92aXRlLmNvbmZpZy50c1wiO2ltcG9ydCB7IGRlZmluZUNvbmZpZywgbG9hZEVudiB9IGZyb20gJ3ZpdGUnO1xuaW1wb3J0IHJlYWN0IGZyb20gJ0B2aXRlanMvcGx1Z2luLXJlYWN0JztcbmltcG9ydCB2aXRlVHNjb25maWdQYXRocyBmcm9tICd2aXRlLXRzY29uZmlnLXBhdGhzJztcbmltcG9ydCBzdmdyIGZyb20gJ3ZpdGUtcGx1Z2luLXN2Z3InO1xuaW1wb3J0IHsgbm9kZVBvbHlmaWxscyB9IGZyb20gJ3ZpdGUtcGx1Z2luLW5vZGUtcG9seWZpbGxzJztcbmltcG9ydCBtZHggZnJvbSAnQG1keC1qcy9yb2xsdXAnO1xuaW1wb3J0IHsgdml0ZVN0YXRpY0NvcHkgfSBmcm9tICd2aXRlLXBsdWdpbi1zdGF0aWMtY29weSc7XG5pbXBvcnQgeyByZXNvbHZlIH0gZnJvbSAncGF0aCc7XG5cbmltcG9ydCB7IGV4dHJhY3REYXRhQ3kgfSBmcm9tICcuL2RhdGFDeS5wbHVnaW4nO1xuaW1wb3J0IHJlaHlwZUhpZ2hsaWdodCBmcm9tICdyZWh5cGUtaGlnaGxpZ2h0JztcblxuZXhwb3J0IGRlZmF1bHQgZGVmaW5lQ29uZmlnKCh7IG1vZGUgfSkgPT4ge1xuICBwcm9jZXNzLmVudiA9IHsgLi4ucHJvY2Vzcy5lbnYsIC4uLmxvYWRFbnYobW9kZSwgcHJvY2Vzcy5jd2QoKSkgfTtcblxuICByZXR1cm4ge1xuICAgIC8vIGRlcGVuZGluZyBvbiB5b3VyIGFwcGxpY2F0aW9uLCBiYXNlIGNhbiBhbHNvIGJlIFwiL1wiXG4gICAgYmFzZTogJy8nLFxuICAgIHBsdWdpbnM6IFtcbiAgICAgIHJlYWN0KCksXG4gICAgICB2aXRlVHNjb25maWdQYXRocygpLFxuICAgICAgc3ZncigpLFxuICAgICAgbWR4KHsgcmVoeXBlUGx1Z2luczogW3JlaHlwZUhpZ2hsaWdodF0gfSksXG4gICAgICBub2RlUG9seWZpbGxzKCksXG4gICAgICBleHRyYWN0RGF0YUN5KCksXG4gICAgICB2aXRlU3RhdGljQ29weSh7XG4gICAgICAgIHRhcmdldHM6IFtcbiAgICAgICAgICB7XG4gICAgICAgICAgICBzcmM6IHJlc29sdmUoJ25vZGVfbW9kdWxlcy9AdGdpbnRlcm5hbC9sYW5ndWFnZS11dGlsL2ZsYWdzJyksXG4gICAgICAgICAgICBkZXN0OiAnJyxcbiAgICAgICAgICB9LFxuICAgICAgICBdLFxuICAgICAgfSksXG4gICAgXSxcbiAgICBzZXJ2ZXI6IHtcbiAgICAgIC8vIHRoaXMgZW5zdXJlcyB0aGF0IHRoZSBicm93c2VyIG9wZW5zIHVwb24gc2VydmVyIHN0YXJ0XG4gICAgICBvcGVuOiB0cnVlLFxuICAgICAgLy8gdGhpcyBzZXRzIGEgZGVmYXVsdCBwb3J0IHRvIDMwMDBcbiAgICAgIHBvcnQ6IE51bWJlcihwcm9jZXNzLmVudi5WSVRFX1BPUlQpIHx8IDMwMDAsXG4gICAgfSxcbiAgfTtcbn0pO1xuIiwgImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCIvVXNlcnMvaHVnbHgvdG9sZ2VlL3RvbGdlZS1wbGF0Zm9ybS93ZWJhcHBcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIi9Vc2Vycy9odWdseC90b2xnZWUvdG9sZ2VlLXBsYXRmb3JtL3dlYmFwcC9kYXRhQ3kucGx1Z2luLnRzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ltcG9ydF9tZXRhX3VybCA9IFwiZmlsZTovLy9Vc2Vycy9odWdseC90b2xnZWUvdG9sZ2VlLXBsYXRmb3JtL3dlYmFwcC9kYXRhQ3kucGx1Z2luLnRzXCI7aW1wb3J0IHsgUGx1Z2luIH0gZnJvbSAndml0ZSc7XG5pbXBvcnQgeyByZWFkZGlyLCByZWFkRmlsZSwgd3JpdGVGaWxlIH0gZnJvbSAnZnMvcHJvbWlzZXMnO1xuaW1wb3J0IHsgcmVzb2x2ZSB9IGZyb20gJ3BhdGgnO1xuaW1wb3J0IHsgZXhpc3RzU3luYyB9IGZyb20gJ2ZzJztcblxuY29uc3QgU1JDX1BBVEggPSByZXNvbHZlKCcuL3NyYycpO1xuXG5leHBvcnQgZnVuY3Rpb24gZXh0cmFjdERhdGFDeSgpOiBQbHVnaW4ge1xuICBjb25zdCBmaWxlSXRlbXM6IFJlY29yZDxzdHJpbmcsIHN0cmluZ1tdPiA9IHt9O1xuXG4gIGFzeW5jIGZ1bmN0aW9uIGdlbmVyYXRlKGZpbGVzOiBzdHJpbmdbXSkge1xuICAgIGF3YWl0IHByb2Nlc3NGaWxlcyhmaWxlcyk7XG4gICAgY29uc3Qgc29ydGVkSXRlbXMgPSBnZXRTb3J0ZWRJdGVtcygpO1xuICAgIGNvbnN0IGZpbGVDb250ZW50ID0gYXdhaXQgZ2VuZXJhdGVGaWxlQ29udGVudChzb3J0ZWRJdGVtcyk7XG4gICAgYXdhaXQgd3JpdGVUb0ZpbGUoZmlsZUNvbnRlbnQpO1xuICB9XG5cbiAgYXN5bmMgZnVuY3Rpb24gcHJvY2Vzc0ZpbGVzKGZpbGVzOiBzdHJpbmdbXSkge1xuICAgIGZvciAoY29uc3QgZmlsZSBvZiBmaWxlcykge1xuICAgICAgYXdhaXQgcHJvY2Vzc0ZpbGUoZmlsZSk7XG4gICAgfVxuICB9XG5cbiAgYXN5bmMgZnVuY3Rpb24gd3JpdGVUb0ZpbGUoZmlsZUNvbnRlbnQ6IHN0cmluZykge1xuICAgIGF3YWl0IHdyaXRlRmlsZShcbiAgICAgIHJlc29sdmUoYC4uL2UyZS9jeXByZXNzL3N1cHBvcnQvZGF0YUN5VHlwZS5kLnRzYCksXG4gICAgICBmaWxlQ29udGVudFxuICAgICk7XG4gIH1cblxuICBhc3luYyBmdW5jdGlvbiBnZW5lcmF0ZUZpbGVDb250ZW50KHNvcnRlZEl0ZW1zKSB7XG4gICAgbGV0IGZpbGVDb250ZW50ID0gJ2RlY2xhcmUgbmFtZXNwYWNlIERhdGFDeSB7XFxuJztcbiAgICBmaWxlQ29udGVudCArPVxuICAgICAgJyAgICBleHBvcnQgdHlwZSBWYWx1ZSA9IFxcbiAgICAgICAgJyArXG4gICAgICBzb3J0ZWRJdGVtcy5tYXAoKGkpID0+IGBcIiR7aX1cImApLmpvaW4oJyB8XFxuICAgICAgICAnKSArXG4gICAgICAnXFxufSc7XG4gICAgcmV0dXJuIGZpbGVDb250ZW50O1xuICB9XG5cbiAgZnVuY3Rpb24gZ2V0U29ydGVkSXRlbXMoKSB7XG4gICAgY29uc3QgaXRlbXMgPSBPYmplY3QudmFsdWVzKGZpbGVJdGVtcykucmVkdWNlKFxuICAgICAgKGFjYywgY3VycikgPT4gWy4uLmFjYywgLi4uY3Vycl0sXG4gICAgICBbXVxuICAgICk7XG4gICAgY29uc3QgaXRlbXNTZXQgPSBuZXcgU2V0KGl0ZW1zKTtcbiAgICByZXR1cm4gWy4uLml0ZW1zU2V0XS5zb3J0KCk7XG4gIH1cblxuICBhc3luYyBmdW5jdGlvbiBwcm9jZXNzRmlsZShmaWxlOiBzdHJpbmcpIHtcbiAgICBpZiAoLy4qXFwudHN4PyQvLnRlc3QoZmlsZSkpIHtcbiAgICAgIGlmICghZXhpc3RzU3luYyhmaWxlKSkge1xuICAgICAgICBmaWxlSXRlbXNbZmlsZV0gPSBbXTtcbiAgICAgICAgcmV0dXJuO1xuICAgICAgfVxuICAgICAgY29uc3QgY29udGVudCA9IChhd2FpdCByZWFkRmlsZShmaWxlKSkudG9TdHJpbmcoKTtcbiAgICAgIGNvbnN0IG1hdGNoZXMgPSBjb250ZW50Lm1hdGNoQWxsKFxuICAgICAgICAvW1wiJ10/ZGF0YS0/W2N8Q115W1wiJ10/XFxzKls9Ol1cXHMqez9bXCInYF0oW0EtWmEtejAtOS1fXFxzXSspW1wiJ2BdP30/L2dcbiAgICAgICk7XG4gICAgICBmaWxlSXRlbXNbZmlsZV0gPSBbXTtcbiAgICAgIGZvciAoY29uc3QgbWF0Y2ggb2YgbWF0Y2hlcykge1xuICAgICAgICBmaWxlSXRlbXNbZmlsZV0ucHVzaChtYXRjaFsxXSk7XG4gICAgICB9XG4gICAgfVxuICB9XG5cbiAgYXN5bmMgZnVuY3Rpb24gZ2V0RmlsZXMoZGlyOiBzdHJpbmcpIHtcbiAgICBjb25zdCBkaXJlbnRzID0gYXdhaXQgcmVhZGRpcihkaXIsIHsgd2l0aEZpbGVUeXBlczogdHJ1ZSB9KTtcbiAgICBjb25zdCBmaWxlcyA9IGF3YWl0IFByb21pc2UuYWxsKFxuICAgICAgZGlyZW50cy5tYXAoKGRpcmVudCkgPT4ge1xuICAgICAgICBjb25zdCByZXMgPSByZXNvbHZlKGRpciwgZGlyZW50Lm5hbWUpO1xuICAgICAgICByZXR1cm4gZGlyZW50LmlzRGlyZWN0b3J5KCkgPyBnZXRGaWxlcyhyZXMpIDogcmVzO1xuICAgICAgfSlcbiAgICApO1xuICAgIHJldHVybiBBcnJheS5wcm90b3R5cGUuY29uY2F0KC4uLmZpbGVzKTtcbiAgfVxuXG4gIHJldHVybiB7XG4gICAgbmFtZTogJ2V4dHJhY3QtZGF0YS1jeScsXG4gICAgYXN5bmMgYnVpbGRTdGFydCgpIHtcbiAgICAgIGNvbnN0IGZpbGVzID0gYXdhaXQgZ2V0RmlsZXMoU1JDX1BBVEgpO1xuICAgICAgYXdhaXQgZ2VuZXJhdGUoZmlsZXMpO1xuICAgIH0sXG4gICAgYXN5bmMgd2F0Y2hDaGFuZ2UoaWQpIHtcbiAgICAgIGdlbmVyYXRlKFtpZF0pO1xuICAgIH0sXG4gIH07XG59XG4iXSwKICAibWFwcGluZ3MiOiAiO0FBQWdULFNBQVMsY0FBYyxlQUFlO0FBQ3RWLE9BQU8sV0FBVztBQUNsQixPQUFPLHVCQUF1QjtBQUM5QixPQUFPLFVBQVU7QUFDakIsU0FBUyxxQkFBcUI7QUFDOUIsT0FBTyxTQUFTO0FBQ2hCLFNBQVMsc0JBQXNCO0FBQy9CLFNBQVMsV0FBQUEsZ0JBQWU7OztBQ054QixTQUFTLFNBQVMsVUFBVSxpQkFBaUI7QUFDN0MsU0FBUyxlQUFlO0FBQ3hCLFNBQVMsa0JBQWtCO0FBRTNCLElBQU0sV0FBVyxRQUFRLE9BQU87QUFFekIsU0FBUyxnQkFBd0I7QUFDdEMsUUFBTSxZQUFzQyxDQUFDO0FBRTdDLGlCQUFlLFNBQVMsT0FBaUI7QUFDdkMsVUFBTSxhQUFhLEtBQUs7QUFDeEIsVUFBTSxjQUFjLGVBQWU7QUFDbkMsVUFBTSxjQUFjLE1BQU0sb0JBQW9CLFdBQVc7QUFDekQsVUFBTSxZQUFZLFdBQVc7QUFBQSxFQUMvQjtBQUVBLGlCQUFlLGFBQWEsT0FBaUI7QUFDM0MsZUFBVyxRQUFRLE9BQU87QUFDeEIsWUFBTSxZQUFZLElBQUk7QUFBQSxJQUN4QjtBQUFBLEVBQ0Y7QUFFQSxpQkFBZSxZQUFZLGFBQXFCO0FBQzlDLFVBQU07QUFBQSxNQUNKLFFBQVEsd0NBQXdDO0FBQUEsTUFDaEQ7QUFBQSxJQUNGO0FBQUEsRUFDRjtBQUVBLGlCQUFlLG9CQUFvQixhQUFhO0FBQzlDLFFBQUksY0FBYztBQUNsQixtQkFDRSx1Q0FDQSxZQUFZLElBQUksQ0FBQyxNQUFNLElBQUksQ0FBQyxHQUFHLEVBQUUsS0FBSyxjQUFjLElBQ3BEO0FBQ0YsV0FBTztBQUFBLEVBQ1Q7QUFFQSxXQUFTLGlCQUFpQjtBQUN4QixVQUFNLFFBQVEsT0FBTyxPQUFPLFNBQVMsRUFBRTtBQUFBLE1BQ3JDLENBQUMsS0FBSyxTQUFTLENBQUMsR0FBRyxLQUFLLEdBQUcsSUFBSTtBQUFBLE1BQy9CLENBQUM7QUFBQSxJQUNIO0FBQ0EsVUFBTSxXQUFXLElBQUksSUFBSSxLQUFLO0FBQzlCLFdBQU8sQ0FBQyxHQUFHLFFBQVEsRUFBRSxLQUFLO0FBQUEsRUFDNUI7QUFFQSxpQkFBZSxZQUFZLE1BQWM7QUFDdkMsUUFBSSxZQUFZLEtBQUssSUFBSSxHQUFHO0FBQzFCLFVBQUksQ0FBQyxXQUFXLElBQUksR0FBRztBQUNyQixrQkFBVSxJQUFJLElBQUksQ0FBQztBQUNuQjtBQUFBLE1BQ0Y7QUFDQSxZQUFNLFdBQVcsTUFBTSxTQUFTLElBQUksR0FBRyxTQUFTO0FBQ2hELFlBQU0sVUFBVSxRQUFRO0FBQUEsUUFDdEI7QUFBQSxNQUNGO0FBQ0EsZ0JBQVUsSUFBSSxJQUFJLENBQUM7QUFDbkIsaUJBQVcsU0FBUyxTQUFTO0FBQzNCLGtCQUFVLElBQUksRUFBRSxLQUFLLE1BQU0sQ0FBQyxDQUFDO0FBQUEsTUFDL0I7QUFBQSxJQUNGO0FBQUEsRUFDRjtBQUVBLGlCQUFlLFNBQVMsS0FBYTtBQUNuQyxVQUFNLFVBQVUsTUFBTSxRQUFRLEtBQUssRUFBRSxlQUFlLEtBQUssQ0FBQztBQUMxRCxVQUFNLFFBQVEsTUFBTSxRQUFRO0FBQUEsTUFDMUIsUUFBUSxJQUFJLENBQUMsV0FBVztBQUN0QixjQUFNLE1BQU0sUUFBUSxLQUFLLE9BQU8sSUFBSTtBQUNwQyxlQUFPLE9BQU8sWUFBWSxJQUFJLFNBQVMsR0FBRyxJQUFJO0FBQUEsTUFDaEQsQ0FBQztBQUFBLElBQ0g7QUFDQSxXQUFPLE1BQU0sVUFBVSxPQUFPLEdBQUcsS0FBSztBQUFBLEVBQ3hDO0FBRUEsU0FBTztBQUFBLElBQ0wsTUFBTTtBQUFBLElBQ04sTUFBTSxhQUFhO0FBQ2pCLFlBQU0sUUFBUSxNQUFNLFNBQVMsUUFBUTtBQUNyQyxZQUFNLFNBQVMsS0FBSztBQUFBLElBQ3RCO0FBQUEsSUFDQSxNQUFNLFlBQVksSUFBSTtBQUNwQixlQUFTLENBQUMsRUFBRSxDQUFDO0FBQUEsSUFDZjtBQUFBLEVBQ0Y7QUFDRjs7O0FENUVBLE9BQU8scUJBQXFCO0FBRTVCLElBQU8sc0JBQVEsYUFBYSxDQUFDLEVBQUUsS0FBSyxNQUFNO0FBQ3hDLFVBQVEsTUFBTSxFQUFFLEdBQUcsUUFBUSxLQUFLLEdBQUcsUUFBUSxNQUFNLFFBQVEsSUFBSSxDQUFDLEVBQUU7QUFFaEUsU0FBTztBQUFBO0FBQUEsSUFFTCxNQUFNO0FBQUEsSUFDTixTQUFTO0FBQUEsTUFDUCxNQUFNO0FBQUEsTUFDTixrQkFBa0I7QUFBQSxNQUNsQixLQUFLO0FBQUEsTUFDTCxJQUFJLEVBQUUsZUFBZSxDQUFDLGVBQWUsRUFBRSxDQUFDO0FBQUEsTUFDeEMsY0FBYztBQUFBLE1BQ2QsY0FBYztBQUFBLE1BQ2QsZUFBZTtBQUFBLFFBQ2IsU0FBUztBQUFBLFVBQ1A7QUFBQSxZQUNFLEtBQUtDLFNBQVEsOENBQThDO0FBQUEsWUFDM0QsTUFBTTtBQUFBLFVBQ1I7QUFBQSxRQUNGO0FBQUEsTUFDRixDQUFDO0FBQUEsSUFDSDtBQUFBLElBQ0EsUUFBUTtBQUFBO0FBQUEsTUFFTixNQUFNO0FBQUE7QUFBQSxNQUVOLE1BQU0sT0FBTyxRQUFRLElBQUksU0FBUyxLQUFLO0FBQUEsSUFDekM7QUFBQSxFQUNGO0FBQ0YsQ0FBQzsiLAogICJuYW1lcyI6IFsicmVzb2x2ZSIsICJyZXNvbHZlIl0KfQo=
