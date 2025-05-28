import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { GlobalError } from 'tg.error/GlobalError';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import React from 'react';

type Props = {
  organizationId?: number;
  glossaryId?: number;
};

type ContextData = {
  glossary: components['schemas']['GlossaryModel'];
};

const ContextHolder = React.createContext<ContextData>(null as any);

export const useGlossaryContext = () => React.useContext(ContextHolder);
export const useGlossary = () => useGlossaryContext().glossary;

export const GlossaryContext: React.FC<Props> = ({
  children,
  organizationId,
  glossaryId,
}) => {
  const dataAvailable =
    organizationId !== undefined && glossaryId !== undefined;

  const glossary = useApiQuery({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}',
    method: 'get',
    path: {
      organizationId: organizationId ?? -1,
      glossaryId: glossaryId ?? -1,
    },
    options: {
      enabled: dataAvailable,
    },
  });

  const isLoading = glossary.isLoading;
  const isWaiting = isLoading || !dataAvailable;

  useGlobalLoading(isWaiting);

  if (isWaiting) {
    return <DashboardPage />;
  }

  if (glossary.error || !glossary.data) {
    throw new GlobalError(
      'Unexpected error occurred',
      glossary.error?.code || 'Loadable error'
    );
  }

  const result: ContextData = {
    glossary: glossary.data,
  };

  return (
    <ContextHolder.Provider value={result as ContextData}>
      {children}
    </ContextHolder.Provider>
  );
};
