import { useEffect } from 'react';
import { GlobalError } from '../error/GlobalError';
import { useApiQuery } from '../service/http/useQueryApi';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { createProvider } from 'tg.fixtures/createProvider';
import { useGlobalLoading } from 'tg.component/GlobalLoading';

type Props = {
  id: number;
};

export const [ProjectContext, useProjectActions, useProjectContext] =
  createProvider(({ id }: Props) => {
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

    const isLoading = project.isLoading || settings.isLoading;

    useGlobalLoading(isLoading);

    if (isLoading) {
      return null;
    }

    if (project.error || settings.error) {
      throw new GlobalError(
        'Unexpected error occurred',
        project.error?.code || settings.error?.code || 'Loadable error'
      );
    }

    const contextData = {
      project: project.data,
      enabledMtServices: settings.data?._embedded?.languageConfigs,
      refetchSettings: settings.refetch,
    };

    const actions = {
      refetchSettings: settings.refetch,
    };

    return [contextData, actions];
  });
