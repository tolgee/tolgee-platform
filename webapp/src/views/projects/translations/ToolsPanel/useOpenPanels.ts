import { useState } from 'react';
import { useTranslationsSelector } from '../context/TranslationsContext';

const OPEN_PANELS_KEY = '__tolgee_openPanels';

export const useOpenPanels = () => {
  const mode = useTranslationsSelector((c) => c?.cursor?.mode);
  const [openPanels, _setOpenPanels] = useState<string[]>(() => {
    let result;
    try {
      result = JSON.parse(localStorage.getItem(OPEN_PANELS_KEY) ?? '');
    } catch (e) {
      // pass
    }
    if (!Array.isArray(result)) {
      result = ['machine_translation', 'translation_memory', 'source_context'];
    }

    if (localStorage.getItem('__tolgee_context_panel_seen') === null) {
      result = [...new Set([...result, 'source_context'])];
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
    localStorage.setItem('__tolgee_context_panel_seen', '1');
    _setOpenPanels(value);
    localStorage.setItem(OPEN_PANELS_KEY, JSON.stringify(value));
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
