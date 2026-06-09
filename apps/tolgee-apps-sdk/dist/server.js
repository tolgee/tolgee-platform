// src/server/verifyWebhookSignature.ts
async function verifyWebhookSignature(opts) {
  if (!opts.header) return false;
  let parsed;
  try {
    parsed = JSON.parse(opts.header);
  } catch {
    return false;
  }
  const { timestamp, signature } = parsed;
  if (typeof timestamp !== "number" || typeof signature !== "string") {
    return false;
  }
  const encoder = new TextEncoder();
  const key = await crypto.subtle.importKey(
    "raw",
    encoder.encode(opts.secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const sigBytes = await crypto.subtle.sign(
    "HMAC",
    key,
    encoder.encode(`${timestamp}.${opts.rawBody}`)
  );
  const expected = hexEncode(new Uint8Array(sigBytes));
  return timingSafeStringEqual(expected, signature);
}
var hexEncode = (bytes) => {
  let out = "";
  for (const b of bytes) out += b.toString(16).padStart(2, "0");
  return out;
};
var timingSafeStringEqual = (a, b) => {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) {
    diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  }
  return diff === 0;
};

// src/server/onWebhook.ts
function onWebhook(payload, types, handler) {
  const list = Array.isArray(types) ? types : [types];
  const activityType = payload.activityData?.type;
  if (activityType && list.includes(activityType)) {
    handler(payload);
    return true;
  }
  return false;
}

// src/server/receiveWebhook.ts
async function receiveWebhook(input) {
  if (input.secret) {
    const valid = await verifyWebhookSignature({
      header: input.signatureHeader,
      rawBody: input.rawBody,
      secret: input.secret
    });
    if (!valid) return { ok: false, status: 401, error: "invalid signature" };
  }
  try {
    return { ok: true, payload: JSON.parse(input.rawBody) };
  } catch {
    return { ok: false, status: 400, error: "invalid json" };
  }
}

// src/server/decodeContextToken.ts
var decodeContextToken = (jwt) => {
  const parts = jwt.split(".");
  if (parts.length !== 3) {
    throw new Error("Malformed JWT: expected 3 segments");
  }
  const payload = JSON.parse(base64UrlDecode(parts[1]));
  const installId = payload["tg.app.inst"];
  const projectId = payload["tg.app.proj"];
  if (typeof installId !== "number" || typeof projectId !== "number") {
    throw new Error("Token missing tg.app.inst or tg.app.proj claim");
  }
  return {
    installId,
    projectId,
    userId: Number(payload.sub),
    audience: String(payload.aud),
    expiresAt: Number(payload.exp)
  };
};
var base64UrlDecode = (s) => {
  const normalized = s.replace(/-/g, "+").replace(/_/g, "/");
  const pad = (4 - normalized.length % 4) % 4;
  return atob(normalized + "=".repeat(pad));
};

// src/server/cors.ts
var tolgeeAppCorsHeaders = () => ({
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET,POST,PUT,DELETE,OPTIONS",
  "Access-Control-Allow-Headers": "Authorization,Content-Type",
  "Access-Control-Max-Age": "600"
});

// src/server/renderManifest.ts
var BASE_URL_PLACEHOLDER = "__BASE_URL__";
var renderManifest = (template, baseUrl) => template.replaceAll(BASE_URL_PLACEHOLDER, baseUrl);

// src/server/config.ts
var loadTolgeeAppConfig = (env = process.env) => ({
  tolgeeUrl: env.TOLGEE_URL ?? "https://app.tolgee.io",
  webhookSecret: env.TOLGEE_WEBHOOK_SECRET ?? null,
  vitePort: Number(env.VITE_PORT ?? 5180),
  serverPort: Number(env.SERVER_PORT ?? env.PORT ?? 5181)
});
export {
  decodeContextToken,
  loadTolgeeAppConfig,
  onWebhook,
  receiveWebhook,
  renderManifest,
  tolgeeAppCorsHeaders,
  verifyWebhookSignature
};
//# sourceMappingURL=server.js.map