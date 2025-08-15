import { createApiClient, projectIdFromKey } from "./ApiClient";

describe("api client", () => {
  it("creates a client", () => {
    const client = createApiClient({ baseUrl: "test" });
    client.setProjectId(10);
    expect(client.getSettings().baseUrl).toEqual("test");
    expect(client.getProjectId()).toEqual(10);
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
});
