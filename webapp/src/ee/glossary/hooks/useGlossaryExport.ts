import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';
import { useApiMutation } from 'tg.service/http/useQueryApi';

const downloadExported = async (response: Response, glossaryName: string) => {
  const data = await response.blob();
  const url = URL.createObjectURL(data);
  try {
    const a = document.createElement('a');
    try {
      a.href = url;
      const dateStr = new Date().toISOString().split('T')[0];
      a.download = 'glossary_' + glossaryName + '_' + dateStr + '.csv';
      a.click();
    } finally {
      a.remove();
    }
  } finally {
    setTimeout(() => URL.revokeObjectURL(url), 7000);
  }
};

export const useGlossaryExport = () => {
  const glossary = useGlossary();

  const exportLoadable = useApiMutation({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/export',
    method: 'get',
    fetchOptions: {
      rawResponse: true,
    },
  });

  const triggerExport = () => {
    exportLoadable.mutate(
      {
        path: {
          organizationId: glossary.organizationOwner.id,
          glossaryId: glossary.id,
        },
      },
      {
        async onSuccess(response) {
          return downloadExported(
            response as unknown as Response,
            glossary.name
          );
        },
      }
    );
  };

  return {
    triggerExport,
    exportLoading: exportLoadable.isLoading,
  };
};
