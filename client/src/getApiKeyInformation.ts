import createClient from "openapi-fetch";

import { API_KEY_PAK_PREFIX } from "./constants";
import { type paths } from "./schema.generated";
import { handleLoadableError } from "./errorFromLoadable";

export type ApiKeyProject = {
  name: string;
  id: number;
};

export type ApiKeyInfoPat = {
  type: "PAT";
  key: string;
  username: string;
  expires: number;
};

export type ApiKeyInfoPak = {
  type: "PAK";
  key: string;
  username: string;
  project: ApiKeyProject;
  expires: number;
};

export type ApiKeyInfo = ApiKeyInfoPat | ApiKeyInfoPak;

export const getApiKeyInformation = async (
  client: ReturnType<typeof createClient<paths>>,
  key: string
): Promise<ApiKeyInfo> => {
  if (key.startsWith(API_KEY_PAK_PREFIX)) {
    const loadable = await client.GET("/v2/api-keys/current");
    if (loadable.response.status === 401) {
      throw new Error(
        `Couldn't log in: the API key you provided is invalid (${
          new URL(loadable.response.url).host
        }).`
      );
    }
    handleLoadableError(loadable);

    const info = loadable.data!;
    const username = info.userFullName || info.username || "<unknown user>";

    return {
      type: "PAK",
      key: key,
      username: username,
      project: {
        id: info.projectId,
        name: info.projectName,
      },
      expires: info.expiresAt ?? 0,
    };
  } else {
    const loadable = await client.GET("/v2/pats/current");
    if (loadable.response.status === 401) {
      throw new Error(
        `Couldn't log in: the API key you provided is invalid (${
          new URL(loadable.response.url).host
        }).`
      );
    }
    handleLoadableError(loadable);

    const info = loadable.data!;
    const username = info.user.name || info.user.username;

    return {
      type: "PAT",
      key: key,
      username: username,
      expires: info.expiresAt ?? 0,
    };
  }
};
