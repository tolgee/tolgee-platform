import JSZip from 'jszip';

import { readExportManifest } from './readExportManifest';

const makeZip = async (entries: Record<string, string>): Promise<File> => {
  const zip = new JSZip();
  Object.entries(entries).forEach(([name, content]) => zip.file(name, content));
  const blob = await zip.generateAsync({ type: 'blob' });
  return new File([blob], 'export.zip', { type: 'application/zip' });
};

describe('readExportManifest', () => {
  it('parses manifest.json from the zip root', async () => {
    const manifest = {
      schemaVersion: '3.205.5',
      sourceProjectName: 'My Project',
      exportedAt: 1782413628533,
      entityCounts: { Key: 42, Translation: 84 },
    };
    const file = await makeZip({
      'manifest.json': JSON.stringify(manifest),
      'project.json': '{}',
    });

    await expect(readExportManifest(file)).resolves.toEqual(manifest);
  });

  it('rejects when the zip has no manifest.json', async () => {
    const file = await makeZip({ 'project.json': '{}' });

    await expect(readExportManifest(file)).rejects.toThrow('manifest_missing');
  });

  it('rejects when manifest.json is not valid JSON', async () => {
    const file = await makeZip({ 'manifest.json': 'not json' });

    await expect(readExportManifest(file)).rejects.toThrow();
  });

  it('rejects when the file is not a zip', async () => {
    const file = new File(['plain text, not a zip'], 'nope.zip', {
      type: 'application/zip',
    });

    await expect(readExportManifest(file)).rejects.toThrow();
  });
});
