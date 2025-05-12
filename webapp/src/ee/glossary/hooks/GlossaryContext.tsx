import { createProvider } from 'tg.fixtures/createProvider';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { GlobalError } from 'tg.error/GlobalError';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';

type Props = {
  organizationId?: number;
  glossaryId?: number;
};

type ContextData = {
  glossary: components['schemas']['GlossaryModel'];
};

export const [GlossaryContext, useGlossaryActions, useGlossaryContext] =
  createProvider(({ organizationId, glossaryId }: Props) => {
    const dataAvailable =
      organizationId !== undefined && glossaryId !== undefined;

    const glossary = useApiQuery({
      url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}',
      method: 'get',
      path: { organizationId: organizationId!, glossaryId: glossaryId! },
      options: {
        enabled: dataAvailable,
      },
    });

    const isLoading = glossary.isLoading;

    useGlobalLoading(isLoading || !dataAvailable);

    if (isLoading || !dataAvailable) {
      return <DashboardPage />;
    }

    if (glossary.error || !glossary.data) {
      throw new GlobalError(
        'Unexpected error occurred',
        glossary.error?.code || 'Loadable error'
      );
    }

    const contextData: ContextData = {
      glossary: glossary.data,
    };

    const actions = {};

    return [contextData, actions];
  });
