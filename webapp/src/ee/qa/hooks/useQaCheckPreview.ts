import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { useProject } from 'tg.hooks/useProject';
import { useQaPreviewWebsocket } from './useQaPreviewWebsocket';
import { QaPreviewIssue } from 'tg.ee.module/qa/models/QaPreviewWsModels';

type QaCheckPreviewProps = {
  text: string | undefined | null;
  languageTag: string;
  keyId: number;
  enabled?: boolean;
};

type QaCheckPreviewResult = {
  issues: QaPreviewIssue[];
  isLoading: boolean;
};

export const useQaCheckPreview = ({
  text,
  languageTag,
  keyId,
  enabled = true,
}: QaCheckPreviewProps): QaCheckPreviewResult => {
  const { isEnabled } = useEnabledFeatures();
  const qaFeatureEnabled = isEnabled('QA_CHECKS');
  const project = useProject();

  const result = useQaPreviewWebsocket({
    projectId: project!.id,
    keyId,
    languageTag,
    text,
    enabled: qaFeatureEnabled && enabled,
  });

  if (!qaFeatureEnabled || !enabled) {
    return { issues: [], isLoading: false };
  }

  return result;
};
