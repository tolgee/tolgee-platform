import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { GlobalError } from 'tg.error/GlobalError';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import React from 'react';
import { GlossaryProvider } from 'tg.ee.module/glossary/hooks/GlossaryProvider';

type Props = {
  organizationId?: number;
  glossaryId?: number;
  fallback?: React.ReactNode;
};

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
      organizationId: organizationId!,
      glossaryId: glossaryId!,
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

  return (
    <GlossaryProvider
      glossary={glossary.data}
      assignedProjects={assignedProjects.data._embedded?.projects ?? []}
    >
      {children}
    </GlossaryProvider>
  );
};
