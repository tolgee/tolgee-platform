import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { downloadBlobAsFile } from 'tg.fixtures/downloadResponseAsFile';

export const downloadExported = async (
  response: Response,
  glossaryName: string,
  branchName?: string
) => {
  const data = await response.blob();
  const dateStr = new Date().toISOString().split('T')[0];
  const branchStr = branchName ? `(${branchName})` : '';
  downloadBlobAsFile(
    data,
    `glossary_${glossaryName}${branchStr}_${dateStr}.csv`
  );
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
