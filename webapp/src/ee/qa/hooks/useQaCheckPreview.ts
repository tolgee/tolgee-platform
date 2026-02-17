import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useEnabledFeatures } from 'tg.globalContext/helpers';

type QaCheckPreviewProps = {
  text: string | undefined | null;
  languageTag: string;
  keyId: number;
  enabled?: boolean;
};

export type QaCheckResultItem = {
  type: string;
  message: string;
  replacement: string | null;
  positionStart: number;
  positionEnd: number;
};

export const useQaCheckPreview = ({
  text,
  languageTag,
  keyId,
  enabled = true,
}: QaCheckPreviewProps): QaCheckResultItem[] => {
  const { isEnabled } = useEnabledFeatures();
  const qaFeatureEnabled = isEnabled('QA_CHECKS');
  const project = useProject();
  const hasText = text !== undefined && text !== null;

  const results = useApiQuery({
    url: '/v2/projects/{projectId}/qa-check/preview',
    method: 'post',
    path: {
      projectId: project!.id,
    },
    content: {
      'application/json': {
        text: text ?? '',
        languageTag,
        keyId,
      },
    },
    options: {
      enabled: qaFeatureEnabled && hasText && enabled,
      keepPreviousData: true,
      noGlobalLoading: true,
    },
  });

  if (!qaFeatureEnabled || !hasText || !enabled || !results.data) {
    return [];
  }

  return (results.data._embedded?.qaCheckResults ?? []) as QaCheckResultItem[];
};
