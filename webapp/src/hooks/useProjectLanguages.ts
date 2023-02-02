import { useContext, useMemo } from 'react';
import { putBaseLangFirst } from 'tg.fixtures/putBaseLangFirst';
import { GlobalError } from '../error/GlobalError';
import { components } from '../service/apiSchema.generated';
import { ProjectLanguagesContext } from './ProjectLanguagesProvider';
import { useProjectPermissions } from './useProjectPermissions';

type LanguageModel = components['schemas']['LanguageModel'];

export const useProjectLanguages = (): LanguageModel[] => {
  const data = useContext(ProjectLanguagesContext);

  const { satisfiesLanguageAccess } = useProjectPermissions();

  if (!data) {
    throw new GlobalError(
      'Unexpected error',
      'No data in loadable? Did you use provider before using hook?'
    );
  }

  const allowedLanguages = useMemo(() => {
    return (
      data._embedded?.languages?.filter((lang) =>
        satisfiesLanguageAccess('translations.view', lang.id)
      ) || []
    );
  }, [data]);

  return allowedLanguages;
};
