import { createApiClient } from "./ApiClient";

describe("api client", () => {
  it("creates a client", () => {
    const client = createApiClient({ baseUrl: "test" });
    expect(client.getSettings().baseUrl).toEqual("test");
  });
});
