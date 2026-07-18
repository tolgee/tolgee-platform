import { InvalidPlaceholder, getInvalidPlaceholders } from '@tginternal/editor';
import { useMemo } from 'react';
import { useProject } from 'tg.hooks/useProject';

export type Props = {
  currentTranslation: string | undefined;
  nested: boolean;
  enabled: boolean;
};

export const useInvalidPlaceholders = ({
  currentTranslation,
  nested,
  enabled,
}: Props): InvalidPlaceholder[] => {
  const icuPlaceholders = useProject().icuPlaceholders;

  return useMemo(() => {
    if (!enabled || !icuPlaceholders) {
      return [];
    }
    return getInvalidPlaceholders(currentTranslation || '', nested);
  }, [currentTranslation, nested, enabled, icuPlaceholders]);
};
