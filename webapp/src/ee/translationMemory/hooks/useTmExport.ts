import { useApiMutation } from 'tg.service/http/useQueryApi';

// Keep filenames cross-platform-safe: replace path separators and the Windows-reserved set
// with `_`, then collapse whitespace into single underscores. Falls back to `tm` when the
// resulting stem is empty.
const sanitizeForFilename = (raw: string): string => {
  const cleaned = raw
    .replace(/[\\/:*?"<>|]/g, '_')
    .trim()
    .replace(/\s+/g, '_');
  return cleaned || 'tm';
};

const downloadExported = async (response: Response, tmName: string) => {
  const data = await response.blob();
  const url = URL.createObjectURL(data);
  const a = document.createElement('a');
  try {
    a.href = url;
    const dateStr = new Date().toISOString().split('T')[0];
    a.download = `tm_${sanitizeForFilename(tmName)}_${dateStr}.tmx`;
    a.click();
  } finally {
    a.remove();
    URL.revokeObjectURL(url);
  }
};

export const useTmExport = (
  organizationId: number,
  translationMemoryId: number,
  tmName: string
) => {
  const exportLoadable = useApiMutation({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/export',
    method: 'get',
    fetchOptions: {
      rawResponse: true,
    },
  });

  const triggerExport = () => {
    exportLoadable.mutate(
      {
        path: { organizationId, translationMemoryId },
      },
      {
        async onSuccess(response) {
          return downloadExported(response as unknown as Response, tmName);
        },
      }
    );
  };

  return {
    triggerExport,
    exportLoading: exportLoadable.isLoading,
  };
};
