import { Placeholder, getPlaceholders } from '@tginternal/editor';
import { useMemo, useRef } from 'react';
import { useProject } from 'tg.hooks/useProject';

export type Props = {
  baseTranslation: string | undefined;
  currentTranslation: string | undefined;
  nested: boolean;
  enabled: boolean;
};

export const useMissingPlaceholders = ({
  baseTranslation,
  currentTranslation,
  nested,
  enabled,
}: Props) => {
  const project = useProject();

  if (!project.icuPlaceholders) {
    return [];
  }

  const basePlaceholders = useMemo(() => {
    return (enabled && getPlaceholders(baseTranslation || '', nested)) || [];
  }, [baseTranslation, nested, enabled]);

  const lastValidPlaceholders = useRef<Placeholder[]>();

  lastValidPlaceholders.current = useMemo(() => {
    const newPlaceholders = getPlaceholders(currentTranslation || '', nested);
    if (newPlaceholders === null) {
      return lastValidPlaceholders.current;
    } else {
      return newPlaceholders;
    }
  }, [currentTranslation, nested]);

  return useMemo(() => {
    const placeholdersMap = new Map();
    lastValidPlaceholders.current?.forEach((i) => {
      const id = i.normalizedValue;
      const value = placeholdersMap.get(id) ?? 0;
      placeholdersMap.set(id, value + 1);
    });
    return basePlaceholders?.filter((item) => {
      const id = item.normalizedValue;
      const value = placeholdersMap.get(id);
      placeholdersMap.set(id, !value ? 0 : value - 1);
      return !value;
    });
  }, [basePlaceholders, lastValidPlaceholders.current]);
};
