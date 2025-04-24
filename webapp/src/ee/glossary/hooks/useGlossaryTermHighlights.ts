import type { GlossaryTermHighlightsProps } from '../../../eeSetup/EeModuleType';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { useEnabledFeatures } from 'tg.globalContext/helpers';

type GlossaryTermHighlightDto =
  components['schemas']['GlossaryTermHighlightDto'];

export const useGlossaryTermHighlights = ({
  text,
  languageTag,
  enabled = true,
}: GlossaryTermHighlightsProps): GlossaryTermHighlightDto[] => {
  const { isEnabled } = useEnabledFeatures();
  const glossaryFeature = isEnabled('GLOSSARY');
  const project = useProject();
  const hasText = text !== undefined && text !== null && text.length > 0;
  const highlights = useApiQuery({
    url: '/v2/projects/{projectId}/glossary-highlights',
    method: 'get',
    path: {
      projectId: project!.id,
    },
    query: {
      text: text ?? '',
      languageTag,
    },
    options: {
      enabled: glossaryFeature && hasText && enabled,
      keepPreviousData: true,
      noGlobalLoading: true,
    },
  });

  if (!glossaryFeature || !hasText || !enabled || !highlights.data) {
    return [];
  }

  return highlights.data._embedded?.glossaryTermHighlightDtoList ?? [];
};
