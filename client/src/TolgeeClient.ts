import { type ApiClientProps, createApiClient } from "./ApiClient";
import { createExportClient } from "./ExportClient";
import { createImportClient } from "./ImportClient";
import { errorFromLoadable } from "./errorFromLoadable";

export type LoadableData = Awaited<ReturnType<TolgeeClient["GET"]>> & {
  data?: any;
};

export type TolgeeClientProps = ApiClientProps;

export function createTolgeeClient(props: TolgeeClientProps) {
  const apiClient = createApiClient(props);

  return {
    ...apiClient,
    import: createImportClient({ apiClient }),
    export: createExportClient({ apiClient }),
  };
}

export const handleLoadableError = (loadable: LoadableData) => {
  if (loadable.error) {
    throw new Error(errorFromLoadable(loadable));
  }
};

export type TolgeeClient = ReturnType<typeof createTolgeeClient>;
