import { useApiMutation } from 'tg.service/http/useQueryApi';

const downloadExported = async (response: Response, tmName: string) => {
  const data = await response.blob();
  const url = URL.createObjectURL(data);
  try {
    const a = document.createElement('a');
    try {
      a.href = url;
      const dateStr = new Date().toISOString().split('T')[0];
      a.download = `tm_${tmName}_${dateStr}.tmx`;
      a.click();
    } finally {
      a.remove();
    }
  } finally {
    setTimeout(() => URL.revokeObjectURL(url), 7000);
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
