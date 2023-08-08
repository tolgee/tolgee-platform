import { v2apiFetch } from './common';

type Options = {
  usingTranslationMemory: boolean;
  usingMachineTranslation: boolean;
  enableForImport?: boolean;
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
