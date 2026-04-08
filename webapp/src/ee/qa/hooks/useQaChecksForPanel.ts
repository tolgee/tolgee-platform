import { useCallback, useMemo } from 'react';
import { getTolgeeFormat, tolgeeFormatGenerateIcu } from '@tginternal/editor';
import { PanelContentData } from 'tg.views/projects/translations/ToolsPanel/common/types';
import { useQaCheckPreview } from './useQaCheckPreview';
import { QaPreviewIssue } from 'tg.ee.module/qa/models/QaPreviewWsModels';
import { useProject } from 'tg.hooks/useProject';
import { adjustQaIssuesForVariant } from 'tg.fixtures/qaUtils';

export const useQaChecksForPanel = (data: PanelContentData) => {
  const { keyData, language, editingText, activeVariant, isModified } = data;
  const project = useProject();
  const raw = !project.icuPlaceholders;

  // For plurals, reconstruct the full ICU text from all variants
  const text = useMemo(() => {
    if (keyData.keyIsPlural && data.editingFullValue) {
      return tolgeeFormatGenerateIcu(data.editingFullValue, raw);
    }
    return editingText ?? '';
  }, [keyData.keyIsPlural, data.editingFullValue, editingText, raw]);

  const variantOffset = useMemo(() => {
    if (!activeVariant || !keyData.keyIsPlural || !text) return 0;
    const parsed = getTolgeeFormat(text, true, raw);
    return parsed.variantOffsets?.[activeVariant as Intl.LDMLPluralRule] ?? 0;
  }, [activeVariant, text, keyData.keyIsPlural, raw]);

  const translation = keyData.translations[language.tag];
  const allPersistedIssues: QaPreviewIssue[] =
    translation?.qaIssues ?? EMPTY_ISSUES;

  // For plurals, filter persisted issues to the active variant
  const persistedIssues = useMemo(() => {
    if (activeVariant) {
      return allPersistedIssues.filter(
        (i) => i.pluralVariant === activeVariant
      );
    }
    return allPersistedIssues;
  }, [allPersistedIssues, activeVariant]);

  const qaChecksStale = translation?.qaChecksStale ?? false;

  const result = useQaCheckPreview({
    text,
    languageTag: language.tag,
    keyId: keyData.keyId,
    variant: activeVariant,
    enabled: isModified || qaChecksStale,
    initialIssues: persistedIssues,
  });

  // Adjust positions from full-ICU to variant-relative for the panel
  const adjustedIssues = useMemo(
    () => adjustQaIssuesForVariant(result.issues, activeVariant, variantOffset),
    [result.issues, activeVariant, variantOffset]
  );

  const updateIssueState = useCallback(
    (adjustedIssue: QaPreviewIssue, newState: QaPreviewIssue['state']) => {
      // Reverse position adjustment
      const originalIssue =
        variantOffset === 0
          ? adjustedIssue
          : {
              ...adjustedIssue,
              positionStart:
                adjustedIssue.positionStart != null
                  ? adjustedIssue.positionStart + variantOffset
                  : adjustedIssue.positionStart,
              positionEnd:
                adjustedIssue.positionEnd != null
                  ? adjustedIssue.positionEnd + variantOffset
                  : adjustedIssue.positionEnd,
            };
      result.updateIssueState(originalIssue, newState);
    },
    [result.updateIssueState, variantOffset]
  );

  return {
    issues: adjustedIssues,
    isLoading: result.isLoading,
    isDisconnected: result.isDisconnected,
    updateIssueState,
  };
};

const EMPTY_ISSUES: QaPreviewIssue[] = [];
