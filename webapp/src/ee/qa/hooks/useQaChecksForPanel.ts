import { PanelContentData } from 'tg.views/projects/translations/ToolsPanel/common/types';
import { useQaCheckPreview } from './useQaCheckPreview';
import { QaPreviewIssue } from 'tg.ee.module/qa/models/QaPreviewWsModels';

export const useQaChecksForPanel = (data: PanelContentData) => {
  const { keyData, language, editingText, isModified } = data;
  const text = editingText ?? '';
  // TODO: When user is editing plural, either use full ICU form for the plural, or don't generate QA issues, or something like that
  // if we go with no live QA for plurals, then the UI should say so.

  const translation = keyData.translations[language.tag];
  const persistedIssues: QaPreviewIssue[] = translation?.qaIssues ?? [];
  const qaChecksStale = translation?.qaChecksStale ?? false;

  return useQaCheckPreview({
    text,
    languageTag: language.tag,
    keyId: keyData.keyId,
    enabled: isModified || qaChecksStale,
    initialIssues: persistedIssues,
  });
};
