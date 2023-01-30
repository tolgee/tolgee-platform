import { v2apiFetch } from './common';

type Options = {
  usingTranslationMemory: true;
  usingMachineTranslation: true;
};

export const putAutoTranslationsSettings = (
  projectId: number,
  options: Options
) => {
  return v2apiFetch(`projects/${projectId}/auto-translation-settings`, {
    method: 'PUT',
    body: {
      usingTranslationMemory: true,
      usingMachineTranslation: true,
      ...options,
    },
  });
};
