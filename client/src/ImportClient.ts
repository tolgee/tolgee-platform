import type { BodyOf } from "./schema.utils";
import type { ApiClient } from "./ApiClient";
import { pathToPosix } from "./pathToPosix";

type ImportRequest = BodyOf<
  "/v2/projects/{projectId}/single-step-import",
  "post"
>;

export type File = { name: string; data: string | Buffer | Blob };
export type ImportProps = Omit<ImportRequest, "files"> & {
  files: Array<File>;
};

type ImportClientProps = {
  apiClient: ApiClient;
};

export const createImportClient = ({ apiClient }: ImportClientProps) => {
  return {
    async import(data: ImportProps) {
      const body = new FormData();
      for (const file of data.files) {
        // converting paths to posix style, so it's correctly matched on the server
        body.append(
          "files",
          new Blob([file.data as unknown as ArrayBuffer]),
          pathToPosix(file.name)
        );
      }

      data.params.fileMappings = data.params.fileMappings.map((i) => ({
        ...i,
        // converting paths to posix style, so it's correctly matched on the server
        fileName: pathToPosix(i.fileName),
      }));

      body.append("params", JSON.stringify(data.params));

      return apiClient.POST("/v2/projects/{projectId}/single-step-import", {
        params: { path: { projectId: apiClient.getProjectId() } },
        body: body as any,
        bodySerializer: (r: any) => r,
      });
    },
  };
};
