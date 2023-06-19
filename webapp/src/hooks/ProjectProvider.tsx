import React, { createContext } from 'react';
import { useEffect } from 'react';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { GlobalError } from '../error/GlobalError';
import { components } from '../service/apiSchema.generated';
import { useApiQuery } from '../service/http/useQueryApi';
import { usePreferredOrganization } from 'tg.globalContext/helpers';

type ProjectModel = components['schemas']['ProjectModel'];
type LanguageConfigItemModel = components['schemas']['LanguageConfigItemModel'];

export const ProjectContext = createContext<{
  project: ProjectModel;
  enabledMtServices?: LanguageConfigItemModel[];
  refetchSettings: () => void;
} | null>(null);

export const ProjectProvider: React.FC<{ id: number }> = ({ id, children }) => {
  const project = useApiQuery({
    url: '/v2/projects/{projectId}',
    method: 'get',
    path: { projectId: id },
  });

  const settings = useApiQuery({
    url: '/v2/projects/{projectId}/machine-translation-service-settings',
    method: 'get',
    path: { projectId: id },
  });

  const { updatePreferredOrganization } = usePreferredOrganization();

  useEffect(() => {
    if (project.data?.organizationOwner) {
      updatePreferredOrganization(project.data.organizationOwner.id);
    }
  }, [project.data]);

  if (project.isLoading || settings.isLoading) {
    return <FullPageLoading />;
  }

  if (project.data) {
    return (
      <ProjectContext.Provider
        value={{
          project: project.data,
          enabledMtServices: settings.data?._embedded?.languageConfigs,
          refetchSettings: settings.refetch,
        }}
      >
        {children}
      </ProjectContext.Provider>
    );
  }

  if (project.error || settings.error) {
    throw new GlobalError(
      'Unexpected error occurred',
      project.error?.code || settings.error?.code || 'Loadable error'
    );
  }
  return null;
};
