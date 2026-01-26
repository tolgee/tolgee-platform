import type {
  GlossaryTermHighlightsProps,
  GlossaryTermHighlightModel,
} from '../../../eeSetup/EeModuleType';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useEnabledFeatures } from 'tg.globalContext/helpers';

export const useGlossaryTermHighlights = ({
  text,
  languageTag,
  enabled = true,
}: GlossaryTermHighlightsProps): GlossaryTermHighlightModel[] => {
  const { isEnabled } = useEnabledFeatures();
  const glossaryFeatureEnabled = isEnabled('GLOSSARY');
  const project = useProject();
  const hasText = text !== undefined && text !== null && text.length > 0;
  const highlights = useApiQuery({
    url: '/v2/projects/{projectId}/glossary-highlights',
    method: 'post',
    path: {
      projectId: project!.id,
    },
    content: {
      'application/json': {
        languageTag,
        text: text ?? '',
      },
    },
    options: {
      enabled: glossaryFeatureEnabled && hasText && enabled,
      keepPreviousData: true,
      noGlobalLoading: true,
    },
  });

  if (!glossaryFeatureEnabled || !hasText || !enabled || !highlights.data) {
    return [];
  }

  return highlights.data._embedded?.glossaryHighlights ?? [];
};
