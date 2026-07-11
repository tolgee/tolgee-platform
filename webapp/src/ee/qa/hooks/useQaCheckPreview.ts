import { useCallback } from 'react';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { useProject } from 'tg.hooks/useProject';
import { useQaPreviewWebsocket } from './useQaPreviewWebsocket';
import { QaPreviewIssue } from 'tg.ee.module/qa/models/QaPreviewWsModels';
import { useQaDisabledLanguageTags } from 'tg.ee.module/qa/hooks/useQaDisabledLanguageIds';

type QaCheckPreviewProps = {
  text: string | undefined | null;
  languageTag: string;
  keyId: number;
  variant?: string;
  enabled?: boolean;
  initialIssues?: QaPreviewIssue[];
};

type QaCheckPreviewResult = {
  issues: QaPreviewIssue[];
  isLoading: boolean;
  isDisconnected: boolean;
  updateIssueState: (
    issue: QaPreviewIssue,
    newState: QaPreviewIssue['state']
  ) => void;
};

export const useQaCheckPreview = ({
  text,
  languageTag,
  keyId,
  variant,
  enabled = true,
  initialIssues,
}: QaCheckPreviewProps): QaCheckPreviewResult => {
  const { isEnabled } = useEnabledFeatures();
  const qaFeatureEnabled = isEnabled('QA_CHECKS');
  const project = useProject();
  const qaDisabledLanguageTags = useQaDisabledLanguageTags();
  const qaLanguageEnabled = !qaDisabledLanguageTags.has(languageTag);

  const result = useQaPreviewWebsocket({
    projectId: project!.id,
    keyId,
    languageTag,
    text,
    variant,
    enabled: qaFeatureEnabled && qaLanguageEnabled && enabled,
    initialIssues,
  });

  const noopUpdateIssueState = useCallback(() => {}, []);

  if (!qaFeatureEnabled || !qaLanguageEnabled) {
    return {
      issues: [],
      isLoading: false,
      isDisconnected: false,
      updateIssueState: noopUpdateIssueState,
    };
  }

  return result;
};
