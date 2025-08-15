import { createApiClient } from "./ApiClient";

describe("api client", () => {
  it("creates a client", () => {
    const client = createApiClient({ baseUrl: "test" });
    client.setProjectId(10);
    expect(client.getSettings().baseUrl).toEqual("test");
    expect(client.getProjectId()).toEqual(10);
  });
});
