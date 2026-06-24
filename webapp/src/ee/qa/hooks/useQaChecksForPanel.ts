import { useCallback, useMemo, useRef } from 'react';
import { getTolgeeFormat, tolgeeFormatGenerateIcu } from '@tginternal/editor';
import { PanelContentData } from 'tg.views/projects/translations/ToolsPanel/common/types';
import { useQaCheckPreview } from './useQaCheckPreview';
import { QaPreviewIssue } from 'tg.ee.module/qa/models/QaPreviewWsModels';
import { useProject } from 'tg.hooks/useProject';
import { offsetQaIssue } from 'tg.fixtures/qaUtils';

export const useQaChecksForPanel = (data: PanelContentData) => {
  const { keyData, language, editingText, activeVariant, isModified } = data;
  const project = useProject();
  const textIsRaw = !project.icuPlaceholders;

  // For plurals, reconstruct the full ICU text from all variants
  const fullIcuText = useMemo(() => {
    if (keyData.keyIsPlural && data.editingFullValue) {
      return tolgeeFormatGenerateIcu(data.editingFullValue, textIsRaw);
    }
    return editingText ?? '';
  }, [keyData.keyIsPlural, data.editingFullValue, editingText, textIsRaw]);

  const variantOffsets = useMemo(() => {
    if (!keyData.keyIsPlural || !fullIcuText) return undefined;
    const parsed = getTolgeeFormat(fullIcuText, true, textIsRaw);
    return parsed.variantOffsets;
  }, [fullIcuText, keyData.keyIsPlural, textIsRaw]);

  const translation = keyData.translations[language.tag];

  const qaChecksStale = translation?.qaChecksStale ?? false;

  // Once we start using the preview websocket, don't stop until user switches
  // to different translation
  const editingKey = `${keyData.keyId}:${language.tag}`;
  const wsLatchRef = useRef({ key: editingKey, enabled: false });
  if (wsLatchRef.current.key !== editingKey) {
    wsLatchRef.current = { key: editingKey, enabled: false };
  }
  if (isModified || qaChecksStale) {
    wsLatchRef.current.enabled = true;
  }
  const wsEnabled = wsLatchRef.current.enabled;

  const result = useQaCheckPreview({
    text: fullIcuText,
    languageTag: language.tag,
    keyId: keyData.keyId,
    variant: activeVariant,
    enabled: wsEnabled,
    initialIssues: translation?.qaIssues,
  });

  // Adjust positions from full-ICU to variant-relative for the panel
  const adjustedIssues = useMemo(() => {
    return result?.issues?.map((issue) => {
      const offset =
        variantOffsets?.[issue.pluralVariant as Intl.LDMLPluralRule];
      return offsetQaIssue(issue, offset ?? 0);
    });
  }, [result.issues, variantOffsets]);

  const updateIssueState = useCallback(
    (issue: QaPreviewIssue, newState: QaPreviewIssue['state']) => {
      // Reverse position adjustment
      const offset =
        variantOffsets?.[issue.pluralVariant as Intl.LDMLPluralRule];
      result.updateIssueState(offsetQaIssue(issue, -(offset ?? 0)), newState);
    },
    [result.updateIssueState, variantOffsets]
  );

  return {
    issues: adjustedIssues,
    isLoading: result.isLoading,
    isDisconnected: result.isDisconnected,
    updateIssueState,
  };
};
