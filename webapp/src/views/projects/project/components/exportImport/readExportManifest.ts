export type ExportManifest = {
  schemaVersion: string;
  sourceProjectName: string;
  exportedAt: number;
  entityCounts: Record<string, number>;
};

const MANIFEST_ENTRY = 'manifest.json';

export const readExportManifest = async (
  file: File
): Promise<ExportManifest> => {
  const { default: JSZip } = await import('jszip');
  const zip = await JSZip.loadAsync(file);
  const entry = zip.file(MANIFEST_ENTRY);
  if (!entry) {
    throw new Error('manifest_missing');
  }
  return JSON.parse(await entry.async('string')) as ExportManifest;
};
