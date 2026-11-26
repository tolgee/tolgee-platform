import { useMemo } from 'react';
import { getTolgeeFormat } from '@tginternal/editor';

import { useProject } from 'tg.hooks/useProject';

export const useTolgeeFormat = (
  text: string | undefined,
  isPlural: boolean
) => {
  const project = useProject();
  return useMemo(
    () => getTolgeeFormat(text || '', isPlural, !project.icuPlaceholders),
    [text, isPlural, project.icuPlaceholders]
  );
};
