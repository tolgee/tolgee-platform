import { createApiClient, projectIdFromKey } from "./ApiClient";
import { errorFromLoadable, type LoadableData } from "./errorFromLoadable";
import { getUnresolvedConflictsMessage } from "./printFailedKeys";

function makeLoadable(
  status: number,
  body?: { code?: string; params?: unknown[] }
): LoadableData {
  return {
    response: { status, url: "https://example.test/v2/foo" } as Response,
    error: body,
  } as unknown as LoadableData;
}

describe("api client", () => {
  it("creates a client", () => {
    const client = createApiClient({ baseUrl: "test" });
    client.setProjectId(10);
    expect(client.getSettings().baseUrl).toEqual("test");
    expect(client.getProjectId()).toEqual(10);
  });

  it("getSettings exposes printError", () => {
    const printError = () => "custom";
    const client = createApiClient({ baseUrl: "test", printError });
    expect(client.getSettings().printError).toBe(printError);
  });

  it("parses project id from pak", () => {
    const parsed = projectIdFromKey(
      "tgpak_gfpxgyzum5ywsmdemrzg2n3qgazg2mljoztw65lhmm3hg"
    );
    expect(parsed).toEqual(1);
  });

  it("parses undefined from pat", () => {
    const parsed = projectIdFromKey(
      "tgpat_1nukgpcrtnqtfrrq6e78olh5fopcg8e6jl8o2lda9dc1f8q5rt49"
    );
    expect(parsed).toBeUndefined();
  });

  it("parses undefined from nonsense", () => {
    const parsed = projectIdFromKey("asdfasdf");
    expect(parsed).toBeUndefined();
  });

  it("getProjectId throws when no projectId and no PAK", () => {
    const client = createApiClient({ baseUrl: "test" });
    expect(() => client.getProjectId()).toThrow(/Project ID is not available/);
  });

  it("getApiKeyInfo throws when no apiKey configured", () => {
    const client = createApiClient({ baseUrl: "test" });
    expect(() => client.getApiKeyInfo()).toThrow(/API key is not configured/);
  });
});

describe("errorFromLoadable", () => {
  it("maps 401 to a missing-auth message", () => {
    expect(errorFromLoadable(makeLoadable(401))).toMatch(
      /Missing or invalid authentication token/
    );
  });

  it("maps 403 to a forbidden message and surfaces the missing scope", () => {
    const msg = errorFromLoadable(
      makeLoadable(403, { code: "missing_scope", params: ["translations.edit"] })
    );
    expect(msg).toMatch(/You are not allowed to perform this operation/);
    expect(msg).toContain("code: missing_scope");
    expect(msg).toContain("missing scope: translations.edit");
  });

  it("maps 429 to rate-limit, 500 to server error, 503 to unavailable", () => {
    expect(errorFromLoadable(makeLoadable(429))).toMatch(/rate limited/);
    expect(errorFromLoadable(makeLoadable(500))).toMatch(/server error/);
    expect(errorFromLoadable(makeLoadable(503))).toMatch(
      /temporarily unavailable/
    );
  });

  it("appends an unresolved-conflicts summary when the error code matches", () => {
    const conflicts = [
      {
        keyName: "hello",
        keyNamespace: "common",
        language: "en",
        isOverridable: true,
      },
      {
        keyName: "world",
        keyNamespace: undefined,
        language: "de",
        isOverridable: false,
      },
    ];
    const msg = errorFromLoadable(
      makeLoadable(400, {
        code: "conflict_is_not_resolved",
        params: [conflicts],
      })
    );
    expect(msg).toContain("Some translations cannot be updated");
    expect(msg).toContain("hello (namespace: common) en (overridable)");
    expect(msg).toContain("world de");
    expect(msg).toContain("overrideMode: ALL");
  });
});

describe("getUnresolvedConflictsMessage", () => {
  it("renders one line per translation and a hint when any are overridable", () => {
    const message = getUnresolvedConflictsMessage([
      {
        keyName: "k",
        keyNamespace: undefined,
        language: "en",
        isOverridable: true,
      } as never,
    ]);
    expect(message).not.toMatch(/^\n/);
    expect(message).toContain("k en (overridable)");
    expect(message).toContain("overrideMode: ALL");
  });

  it("does not include the hint when nothing is overridable", () => {
    const message = getUnresolvedConflictsMessage([
      {
        keyName: "k",
        keyNamespace: undefined,
        language: "en",
        isOverridable: false,
      } as never,
    ]);
    expect(message).not.toContain("overrideMode: ALL");
  });
});
