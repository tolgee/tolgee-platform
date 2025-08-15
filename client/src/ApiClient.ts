import type { paths } from "./schema.generated";

import createClient, { type ParseAs } from "openapi-fetch";
import base32Decode from "base32-decode";
import { API_KEY_PAK_PREFIX, USER_AGENT } from "./constants";
import { getApiKeyInformation } from "./getApiKeyInformation";
import { errorFromLoadable } from "./errorFromLoadable";
import type { BodyOf } from "./schema.utils";

export type LoadableData = Awaited<ReturnType<ApiClient["GET"]>> & {
  data?: any;
};

async function parseResponse(response: Response, parseAs: ParseAs) {
  // handle empty content
  // note: we return `{}` because we want user truthy checks for `.data` or `.error` to succeed
  if (
    response.status === 204 ||
    response.headers.get("Content-Length") === "0"
  ) {
    return response.ok ? { data: {}, response } : { error: {}, response };
  }

  // parse response (falling back to .text() when necessary)
  if (response.ok) {
    // if "stream", skip parsing entirely
    if (parseAs === "stream") {
      return { data: response.body, response };
    }
    return { data: await response[parseAs](), response };
  }

  // handle errors
  let error = await response.text();
  try {
    error = JSON.parse(error); // attempt to parse as JSON
  } catch {
    // noop
  }
  return { error, response };
}

export function projectIdFromKey(key: string) {
  if (!key.startsWith(API_KEY_PAK_PREFIX)) {
    return undefined;
  }

  const keyBuffer = base32Decode(
    key.slice(API_KEY_PAK_PREFIX.length).toUpperCase(),
    "RFC4648"
  );

  const decoded = Buffer.from(keyBuffer).toString("utf8");
  return Number(decoded.split("_")[0]);
}

export type ApiClientProps = {
  baseUrl: string;
  apiKey?: string;
  projectId?: number | undefined;
  autoThrow?: boolean;
  verbose?: boolean;
  userToken?: string;
  printError?: (loadable: LoadableData) => string;
};

export function createApiClient(props: ApiClientProps) {
  let {
    projectId,
    baseUrl = "https://app.tolgee.io",
    apiKey,
    autoThrow = true,
    userToken,
    verbose,
    printError = errorFromLoadable,
  } = props;
  const apiClient = createClient<paths>({
    baseUrl,
    headers: {
      "user-agent": USER_AGENT,
    },
  });

  function debug(text: string) {
    if (verbose) {
      console.log(text);
    }
  }

  apiClient.use({
    onRequest: ({ request }) => {
      debug(`[HTTP] Requesting: ${request.method} ${request.url}`);
      if (apiKey) {
        request.headers.set("x-api-key", apiKey);
      } else if (userToken) {
        request.headers.set("Authorization", "Bearer " + userToken);
      }
    },
    onResponse: async ({ response, options }) => {
      let responseText = `[HTTP] Response: ${response.url} [${response.status}]`;
      const apiVersion = response.headers.get("x-tolgee-version");
      if (apiVersion) {
        responseText += ` [${response.headers.get("x-tolgee-version")}]`;
      }
      if (!response.ok && verbose && response.body) {
        const clonedBody = await response.clone().text();
        if (clonedBody) {
          responseText += ` [${clonedBody}]`;
        }
      }
      debug(responseText);
      if (autoThrow && !response.ok) {
        const loadable = await parseResponse(response, options.parseAs);
        throw new Error(
          `Tolgee request error ${response.url} ${errorFromLoadable(
            loadable as any
          )}`
        );
      }
    },
  });

  const self = {
    ...apiClient,
    getProjectId() {
      return projectId ?? (apiKey ? projectIdFromKey(apiKey) : undefined);
    },
    getApiKeyInfo() {
      return getApiKeyInformation(apiClient, apiKey!);
    },
    getSettings(): ApiClientProps {
      return { projectId, baseUrl, apiKey, autoThrow, userToken, verbose };
    },
    setProjectId(id: number) {
      projectId = id;
    },
    setApiKey(key: string) {
      apiKey = key;
    },
    setUserToken(token: string) {
      userToken = token;
    },
    async login(body: BodyOf<"/api/public/generatetoken", "post">) {
      const response = await apiClient.POST("/api/public/generatetoken", {
        body,
      });
      if (response.data?.accessToken) {
        self.setUserToken(response.data.accessToken);
      } else if (response.error) {
        printError(response.error as any);
      } else {
        throw Error("Couldn't fetch access token", response.error);
      }
      return response;
    },
  };
  return self;
}

export type ApiClient = ReturnType<typeof createApiClient>;
