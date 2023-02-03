import { useContext } from 'react';
import { GlobalError } from '../error/GlobalError';
import { components } from '../service/apiSchema.generated';
import { ProjectLanguagesContext } from './ProjectLanguagesProvider';

type LanguageModel = components['schemas']['LanguageModel'];

export const useProjectLanguages = (): LanguageModel[] => {
  const data = useContext(ProjectLanguagesContext);

  if (!data) {
    throw new GlobalError(
      'Unexpected error',
      'No data in loadable? Did you use provider before using hook?'
    );
  }

  return data._embedded?.languages || [];
};
