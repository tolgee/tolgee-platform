import { GlobalError } from '../error/GlobalError';
import { components } from '../service/apiSchema.generated';
import { ProjectLanguagesContext } from './ProjectLanguagesProvider';
import { useContext } from 'react';

type LanguageModel = components['schemas']['LanguageModel'];

export const useProjectLanguages = (): LanguageModel[] => {
  const data = useContext(ProjectLanguagesContext);

  if (!data) {
    throw new GlobalError(
      'Unexpected error',
      'No data in loadable? Did you use provider before using hook?'
    );
  }

  if (!data._embedded?.languages) {
    return [];
  }

  return data._embedded.languages;
};
