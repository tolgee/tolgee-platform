import { getTolgeeFormat } from '@tginternal/editor';
import { useMemo } from 'react';
import { useProject } from 'tg.hooks/useProject';

export const useBaseTranslation = (
  activeVariant: string | undefined,
  baseTranslation: string | undefined,
  isPlural: boolean
) => {
  const project = useProject();
  return useMemo(() => {
    if (activeVariant) {
      const variants = getTolgeeFormat(
        baseTranslation || '',
        isPlural,
        !project.icuPlaceholders
      )?.variants;
      return variants?.[activeVariant] ?? variants?.['other'];
    } else {
      return baseTranslation;
    }
  }, [baseTranslation, activeVariant]);
};
