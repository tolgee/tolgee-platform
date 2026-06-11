#!/usr/bin/env node

// src/lib.ts
import { mkdirSync, readFileSync, writeFileSync } from "fs";
import { join } from "path";
var STATE_DIR = join(process.cwd(), ".tolgee-dev");
var INSTALL_FILE = join(STATE_DIR, "install.json");
var TUNNEL_FILE = join(STATE_DIR, "tunnel.json");
var writeJson = (path, value) => {
  mkdirSync(STATE_DIR, { recursive: true });
  writeFileSync(path, JSON.stringify(value, null, 2) + "\n", "utf8");
};
var readInstallState = () => {
  try {
    return JSON.parse(readFileSync(INSTALL_FILE, "utf8"));
  } catch {
    return null;
  }
};
var writeTunnelState = (state) => {
  writeJson(TUNNEL_FILE, state);
};
var clearTunnelState = () => {
  const vitePort3 = Number(process.env.VITE_PORT ?? 5180);
  writeJson(TUNNEL_FILE, { baseUrl: `http://localhost:${vitePort3}` });
};
var readTunnelState = () => {
  try {
    return JSON.parse(readFileSync(TUNNEL_FILE, "utf8"));
  } catch {
    return null;
  }
};
var patchManifestUrl = async (install3, manifestUrl) => {
  const url = `${install3.tolgeeUrl}/v2/apps/self/manifest-url`;
  const res = await fetch(url, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      "X-API-Key": install3.clientSecret
    },
    body: JSON.stringify({ manifestUrl })
  });
  if (!res.ok) {
    throw new Error(
      `PATCH manifest-url failed (${res.status}): ${await res.text()}`
    );
  }
};
var isLocalHost = (urlString) => {
  try {
    const host = new URL(urlString).hostname;
    return host === "localhost" || host === "127.0.0.1" || host === "::1";
  } catch {
    return false;
  }
};
var loadEnvLocal = () => {
  try {
    process.loadEnvFile(join(process.cwd(), ".env.local"));
  } catch {
  }
};

// src/dev.ts
import { existsSync } from "fs";
import { bin, install, Tunnel } from "cloudflared";
var vitePort = () => Number(process.env.VITE_PORT ?? 5180);
var localBaseUrl = () => `http://localhost:${vitePort()}`;
var shouldSkipTunnel = (installState) => {
  if (process.env.TOLGEE_DEV_TUNNEL === "none") return true;
  if (installState && isLocalHost(installState.tolgeeUrl)) return true;
  if (process.env.TOLGEE_URL && isLocalHost(process.env.TOLGEE_URL)) return true;
  return false;
};
var ensureBinary = async () => {
  if (!existsSync(bin)) {
    console.log("[tunnel] installing cloudflared binary\u2026");
    await install(bin);
  }
};
var startTunnel = async () => {
  const tunnel = Tunnel.quick(`http://localhost:${vitePort()}`);
  const url = await new Promise((resolve) => {
    tunnel.once("url", resolve);
  });
  tunnel.on("exit", (code) => {
    console.log(`[tunnel] cloudflared exited with code ${code}`);
  });
  process.once("SIGINT", () => tunnel.stop());
  process.once("SIGTERM", () => tunnel.stop());
  return url;
};
var maybePatchManifestUrl = async (installState, baseUrl) => {
  if (!installState) {
    console.log(
      "[tunnel] no install registered \u2014 run `tolgee-app register` to install the plugin against a Tolgee organization."
    );
    return;
  }
  try {
    await patchManifestUrl(installState, `${baseUrl}/manifest.json`);
    console.log(
      `[tunnel] PATCHed install ${installState.installId} to use ${baseUrl}/manifest.json`
    );
  } catch (err) {
    console.error("[tunnel] manifest-url update failed:", err);
  }
};
var runDev = async () => {
  clearTunnelState();
  const installState = readInstallState();
  if (shouldSkipTunnel(installState)) {
    console.log(
      `[tunnel] skipping cloudflared \u2014 Tolgee reaches the plugin at ${localBaseUrl()}`
    );
    writeTunnelState({ baseUrl: localBaseUrl() });
    await maybePatchManifestUrl(installState, localBaseUrl());
    const heartbeat = setInterval(() => {
    }, 1 << 30);
    await new Promise((resolve) => {
      process.once("SIGINT", () => resolve());
      process.once("SIGTERM", () => resolve());
    });
    clearInterval(heartbeat);
    return;
  }
  await ensureBinary();
  const baseUrl = await startTunnel();
  console.log(`[tunnel] live at ${baseUrl}`);
  writeTunnelState({ baseUrl });
  await maybePatchManifestUrl(installState, baseUrl);
};

// src/register.ts
import { createInterface } from "readline/promises";
import { existsSync as existsSync2 } from "fs";
import {
  createServer
} from "http";
import { randomUUID, timingSafeEqual } from "crypto";
import open from "open";
import { bin as bin2, install as install2, Tunnel as Tunnel2 } from "cloudflared";
var sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
var BROWSER_TIMEOUT_MS = 5 * 6e4;
var vitePort2 = () => Number(process.env.VITE_PORT ?? 5180);
var serverPort = () => Number(process.env.SERVER_PORT ?? process.env.PORT ?? 5181);
var verifyManifestReachable = async (manifestUrl) => {
  const attempts = 5;
  let lastError;
  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      const res = await fetch(manifestUrl);
      if (res.ok) return;
      lastError = new Error(`${manifestUrl} returned ${res.status}`);
    } catch (err) {
      lastError = err;
    }
    if (attempt < attempts) await sleep(1e3);
  }
  throw new Error(
    `Could not reach ${manifestUrl}. Is \`npm run dev\` running in another terminal? (${lastError instanceof Error ? lastError.message : String(lastError)})`
  );
};
var startTunnel2 = async () => {
  if (!existsSync2(bin2)) {
    console.log("Installing cloudflared binary\u2026");
    await install2(bin2);
  }
  console.log("Starting Cloudflare quick tunnel\u2026");
  const tunnel = Tunnel2.quick(`http://localhost:${vitePort2()}`);
  const baseUrl = await new Promise(
    (resolve) => tunnel.once("url", resolve)
  );
  console.log(`Tunnel live at ${baseUrl}`);
  return baseUrl;
};
var reusableTunnel = async () => {
  const baseUrl = readTunnelState()?.baseUrl;
  if (!baseUrl || isLocalHost(baseUrl)) return null;
  try {
    const res = await fetch(`${baseUrl}/manifest.json`);
    if (res.ok) {
      console.log(`Reusing live tunnel from \`tolgee-app dev\`: ${baseUrl}`);
      return baseUrl;
    }
  } catch {
  }
  return null;
};
var resolveManifestUrl = async (tolgeeUrl) => {
  const localManifestUrl = `http://localhost:${serverPort()}/manifest.json`;
  if (isLocalHost(tolgeeUrl)) {
    writeTunnelState({ baseUrl: `http://localhost:${vitePort2()}` });
    console.log(`Skipping tunnel \u2014 Tolgee is local; using ${localManifestUrl}`);
    return localManifestUrl;
  }
  const base = await reusableTunnel() ?? await startTunnel2();
  writeTunnelState({ baseUrl: base });
  return `${base}/manifest.json`;
};
var constantTimeEq = (a, b) => {
  const ab = Buffer.from(a);
  const bb = Buffer.from(b);
  if (ab.length !== bb.length) return false;
  return timingSafeEqual(ab, bb);
};
var browserRegister = async (tolgeeUrl, manifestUrl) => {
  const state = randomUUID();
  return await new Promise((resolve, reject) => {
    let settled = false;
    const finish = (kind, value) => {
      if (settled) return;
      settled = true;
      clearTimeout(timeout);
      server.close();
      if (kind === "resolve") resolve(value);
      else reject(value);
    };
    const server = createServer((req, res) => {
      const url = new URL(req.url ?? "", `http://127.0.0.1`);
      if (url.pathname !== "/cb") {
        res.statusCode = 404;
        res.end("Not found");
        return;
      }
      const incomingState = url.searchParams.get("state") ?? "";
      if (!constantTimeEq(incomingState, state)) {
        res.statusCode = 400;
        res.end(htmlPage("State mismatch \u2014 refusing this callback.", false));
        finish("reject", new Error("Callback state did not match."));
        return;
      }
      const error = url.searchParams.get("error");
      if (error) {
        res.end(htmlPage(`Install ${error}. You can close this tab.`, false));
        finish("reject", new Error(`Install ${error}`));
        return;
      }
      const installId = Number(url.searchParams.get("installId"));
      const organizationId = Number(url.searchParams.get("organizationId"));
      const clientId = url.searchParams.get("clientId") ?? "";
      const clientSecret = url.searchParams.get("clientSecret") ?? "";
      const webhookSecret = url.searchParams.get("webhookSecret") ?? "";
      if (!installId || !organizationId || !clientId || !clientSecret) {
        res.statusCode = 400;
        res.end(htmlPage("Callback missing required fields.", false));
        finish("reject", new Error("Callback missing required fields."));
        return;
      }
      res.end(htmlPage("Installed. You can close this tab.", true));
      finish("resolve", {
        tolgeeUrl,
        organizationId,
        installId,
        clientId,
        clientSecret,
        webhookSecret
      });
    });
    const timeout = setTimeout(
      () => finish("reject", new Error("Browser flow timed out.")),
      BROWSER_TIMEOUT_MS
    );
    server.on("error", (err) => finish("reject", err));
    server.listen(0, "127.0.0.1", async () => {
      const address = server.address();
      if (!address || typeof address === "string") {
        finish("reject", new Error("Failed to bind localhost port"));
        return;
      }
      const callback = `http://127.0.0.1:${address.port}/cb`;
      const installUrl = `${tolgeeUrl.replace(/\/$/, "")}/install-app?manifestUrl=${encodeURIComponent(manifestUrl)}&callback=${encodeURIComponent(callback)}&state=${encodeURIComponent(state)}`;
      console.log(`
Opening browser to install the plugin\u2026`);
      console.log(`  ${installUrl}
`);
      console.log(
        `If the browser doesn't open automatically, paste that URL into it.
Waiting up to ${BROWSER_TIMEOUT_MS / 1e3}s for you to approve\u2026`
      );
      try {
        await open(installUrl);
      } catch {
      }
    });
  });
};
var htmlPage = (message, ok) => {
  const color = ok ? "#1b8c43" : "#c44343";
  return `<!doctype html>
<html><head><meta charset="utf-8"><title>Tolgee plugin install</title>
<style>
  body { font-family: -apple-system, system-ui, sans-serif; max-width: 480px; margin: 80px auto; text-align: center; color: #222; }
  .badge { display: inline-block; padding: 12px 18px; border-radius: 999px; color: white; background: ${color}; margin-bottom: 16px; font-weight: 600; }
</style></head><body>
  <div class="badge">${ok ? "\u2713 Done" : "\u2717 Stopped"}</div>
  <p>${message}</p>
</body></html>`;
};
var patRegister = async (tolgeeUrl, manifestUrl, pat) => {
  if (!pat.startsWith("tgpat_")) {
    throw new Error("--pat value must start with tgpat_");
  }
  const orgs = await (async () => {
    const res2 = await fetch(`${tolgeeUrl}/v2/organizations?size=100`, {
      headers: { "X-API-Key": pat }
    });
    if (!res2.ok) {
      throw new Error(
        `Could not list organizations (${res2.status}): ${await res2.text()}`
      );
    }
    const json = await res2.json();
    return json._embedded?.organizations ?? [];
  })();
  if (orgs.length === 0) {
    throw new Error("No organizations visible to this PAT.");
  }
  console.log("\nOrganizations:");
  orgs.forEach((o, i) => console.log(`  ${i + 1}. ${o.name} (${o.slug})`));
  const rl = createInterface({ input: process.stdin, output: process.stdout });
  const idxStr = (await rl.question(`Pick one [1-${orgs.length}]: `)).trim();
  rl.close();
  const idx = Number.parseInt(idxStr, 10) - 1;
  const org = orgs[idx];
  if (!org) throw new Error("Invalid selection");
  const res = await fetch(`${tolgeeUrl}/v2/organizations/${org.id}/apps`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "X-API-Key": pat },
    body: JSON.stringify({ manifestUrl })
  });
  if (!res.ok) {
    throw new Error(`Register failed (${res.status}): ${await res.text()}`);
  }
  const data = await res.json();
  return {
    tolgeeUrl,
    organizationId: org.id,
    installId: data.id,
    clientId: data.clientId,
    clientSecret: data.clientSecret,
    webhookSecret: data.webhookSecret
  };
};
var runRegister = async (args) => {
  const patFlag = args.find((a) => a.startsWith("--pat="))?.slice("--pat=".length) ?? null;
  if (existsSync2(INSTALL_FILE)) {
    console.log(
      `Install record already exists at ${INSTALL_FILE}. Delete it to re-register, or just run \`npm run dev\` to keep using the existing install.`
    );
    return;
  }
  const tolgeeUrl = (process.env.TOLGEE_URL ?? "https://app.tolgee.io").replace(
    /\/$/,
    ""
  );
  const manifestUrl = await resolveManifestUrl(tolgeeUrl);
  await verifyManifestReachable(manifestUrl);
  const state = patFlag ? await patRegister(tolgeeUrl, manifestUrl, patFlag) : await browserRegister(tolgeeUrl, manifestUrl);
  writeJson(INSTALL_FILE, state);
  console.log(
    `
Registered. Install ${state.installId} for org ${state.organizationId}. Saved to ${INSTALL_FILE}.`
  );
};

// src/cli.ts
var USAGE = `tolgee-app \u2014 Tolgee Apps dev toolchain

Usage:
  tolgee-app dev         Boot the tunnel + repoint the install's manifest URL
  tolgee-app register    One-time install against a Tolgee org (browser flow)
                         Pass --pat=tgpat_\u2026 for the headless flow.
`;
var main = async () => {
  loadEnvLocal();
  const [command, ...rest] = process.argv.slice(2);
  if (command === "dev") {
    await runDev();
    return;
  }
  if (command === "register") {
    await runRegister(rest);
    return;
  }
  console.log(USAGE);
  process.exit(command ? 1 : 0);
};
main().catch((err) => {
  console.error(err instanceof Error ? err.message : err);
  process.exit(1);
});
