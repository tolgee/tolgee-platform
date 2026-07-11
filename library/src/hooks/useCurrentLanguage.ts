import { useTolgee } from '@tolgee/react';
import type { locales } from '@tginternal/library/constants/locales';

export const useCurrentLanguage = () => {
  const tolgee = useTolgee(['language']);
  return tolgee.getLanguage() as keyof typeof locales | undefined;
};
