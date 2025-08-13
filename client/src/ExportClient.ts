import type { BodyOf } from "./schema.utils";
import type { Blob } from "buffer";
import { type ApiClient } from "./ApiClient";

export type ExportRequest = Omit<
  BodyOf<"/v2/projects/{projectId}/export", "post">,
  "zip"
>;

type SingleExportRequest = Omit<ExportRequest, "languages"> & {
  languages: [string];
};

type ExportClientProps = {
  apiClient: ApiClient;
};

export const createExportClient = ({ apiClient }: ExportClientProps) => {
  return {
    async export(req: ExportRequest) {
      const body = { ...req, zip: true };
      const loadable = await apiClient.POST("/v2/projects/{projectId}/export", {
        params: { path: { projectId: apiClient.getProjectId() } },
        body: body,
        parseAs: "blob",
      });
      return { ...loadable, data: loadable.data as unknown as Blob };
    },

    async exportSingle(req: SingleExportRequest) {
      return apiClient.POST("/v2/projects/{projectId}/export", {
        params: { path: { projectId: apiClient.getProjectId() } },
        body: { ...req, zip: false },
      });
    },
  };
};

export type ExportClient = ReturnType<typeof createExportClient>;
