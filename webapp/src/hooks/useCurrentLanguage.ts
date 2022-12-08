import { useTolgee } from '@tolgee/react';

export const useCurrentLanguage = () => {
  const tolgee = useTolgee(['language']);
  return tolgee.getLanguage() as string;
};
