import { useState } from 'react';
import { useTranslationsSelector } from '../context/TranslationsContext';

const OPEN_PANELS_KEY = '__tolgee_openPanels';

export const useOpenPanels = () => {
  const mode = useTranslationsSelector((c) => c?.cursor?.mode);
  const [openPanels, _setOpenPanels] = useState<string[]>(() => {
    let result: string[] = [
      'machine_translation',
      'translation_memory',
      'source_context',
    ];
    try {
      const saved = JSON.parse(localStorage.getItem(OPEN_PANELS_KEY) ?? 'null');
      if (Array.isArray(saved)) {
        result =
          localStorage.getItem('__tolgee_context_panel_seen') === null
            ? [...new Set<string>([...saved, 'source_context'])]
            : saved;
      }
    } catch {
      // Use the defaults when preferences cannot be read.
    }
    if (mode === 'comments' && !result.includes('comments')) {
      result.push('comments');
    }

    if (mode === 'qa_checks' && !result.includes('qa_checks')) {
      result.push('qa_checks');
    }
    return result;
  });

  function setOpenPanels(value: string[]) {
    _setOpenPanels(value);
    try {
      localStorage.setItem(OPEN_PANELS_KEY, JSON.stringify(value));
      localStorage.setItem('__tolgee_context_panel_seen', '1');
    } catch {
      // Keep the selection for this session when storage is unavailable.
    }
  }

  function togglePanelOpen(id: string) {
    if (openPanels.includes(id)) {
      setOpenPanels(openPanels.filter((i) => i !== id));
    } else {
      setOpenPanels([...openPanels, id]);
    }
  }

  return { openPanels, setOpenPanels, togglePanelOpen } as const;
};
