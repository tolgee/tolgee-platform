import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { GlobalError } from 'tg.error/GlobalError';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import React, { useMemo } from 'react';

type Props = {
  organizationId?: number;
  glossaryId?: number;
  fallback?: React.ReactNode;
};

type AssignedProjects = {
  assignedProjects: components['schemas']['SimpleProjectModel'][];
};

type ContextData = {
  glossary: components['schemas']['GlossaryModel'] & AssignedProjects;
};

const ContextHolder = React.createContext<ContextData>(null as any);

export const useGlossaryContext = () => React.useContext(ContextHolder);
export const useGlossary = () => useGlossaryContext().glossary;

export const GlossaryContext: React.FC<Props> = ({
  children,
  organizationId,
  glossaryId,
  fallback,
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

  const assignedProjects = useApiQuery({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/assigned-projects',
    method: 'get',
    path: {
      organizationId: organizationId ?? -1,
      glossaryId: glossaryId ?? -1,
    },
    options: {
      enabled: dataAvailable,
    },
  });

  const isLoading = glossary.isLoading || assignedProjects.isLoading;
  const isWaiting = isLoading || !dataAvailable;

  useGlobalLoading(isWaiting);

  if (isWaiting) {
    if (fallback !== undefined) {
      return <>{fallback}</>;
    }
    return <DashboardPage />;
  }

  if (
    glossary.error ||
    !glossary.data ||
    assignedProjects.error ||
    !assignedProjects.data
  ) {
    throw new GlobalError(
      'Unexpected error occurred',
      glossary.error?.code || assignedProjects.error?.code || 'Server error'
    );
  }

  const result: ContextData = useMemo(() => {
    return {
      glossary: {
        ...glossary.data,
        assignedProjects: assignedProjects.data._embedded?.projects || [],
      },
    };
  }, [glossary.data, assignedProjects.data]);

  return (
    <ContextHolder.Provider value={result as ContextData}>
      {children}
    </ContextHolder.Provider>
  );
};
