import { useMemo, useRef } from 'react';
import {
  getTolgeeFormat,
  TolgeeFormat,
  tolgeeFormatGenerateIcu,
} from '@tginternal/editor';
import { PanelContentData } from 'tg.views/projects/translations/ToolsPanel/common/types';
import { useQaCheckPreview } from './useQaCheckPreview';
import { QaPreviewIssue } from 'tg.ee.module/qa/models/QaPreviewWsModels';
import { useProject } from 'tg.hooks/useProject';
import { offsetQaIssue } from 'tg.fixtures/qaUtils';
import { useApiMutation } from 'tg.service/http/useQueryApi';

const variantOffsetOf = (
  variantOffsets: TolgeeFormat['variantOffsets'],
  issue: QaPreviewIssue
) => variantOffsets?.[issue.pluralVariant as Intl.LDMLPluralRule] ?? 0;

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
  const adjustedIssues = useMemo(
    () =>
      result.issues.map((issue) =>
        offsetQaIssue(issue, variantOffsetOf(variantOffsets, issue))
      ),
    [result.issues, variantOffsets]
  );

  const ignoreMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/qa-issues/suppressions',
    method: 'post',
  });

  const unignoreMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/qa-issues/suppressions',
    method: 'delete',
  });

  const toggleIgnore = (issue: QaPreviewIssue) => {
    const translationId = translation?.id;
    if (translationId == null) return;

    const fullTextIssue = offsetQaIssue(
      issue,
      -variantOffsetOf(variantOffsets, issue)
    );
    const isIgnored = issue.state === 'IGNORED';
    const newState = isIgnored ? 'OPEN' : 'IGNORED';
    const mutation = isIgnored ? unignoreMutation : ignoreMutation;
    const { state: _, ...issueRequest } = fullTextIssue;
    mutation.mutate(
      {
        path: { projectId: project.id, translationId },
        content: { 'application/json': issueRequest },
      },
      {
        onSuccess: () => result.updateIssueState(fullTextIssue, newState),
      }
    );
  };

  return {
    issues: adjustedIssues,
    isLoading: result.isLoading,
    isDisconnected: result.isDisconnected,
    toggleIgnore,
  };
};
