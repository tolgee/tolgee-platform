import { styled, Tooltip } from '@mui/material';
import { TooltipCard } from 'tg.component/common/TooltipCard';
import { GlossaryTermPreview } from 'tg.ee';
import { components } from 'tg.service/apiSchema.generated';
import { usePreferredOrganization } from 'tg.globalContext/helpers';

type GlossaryTermModel = components['schemas']['GlossaryTermModel'];

const StyledHighlight = styled('span')`
  text-decoration: underline;
  text-decoration-style: dashed;
  text-underline-offset: ${({ theme }) => theme.spacing(0.5)};
`;

type Props = {
  text: string;
  term: GlossaryTermModel;
  languageTag: string;
  targetLanguageTag?: string;
  onTranslationUpdated?: () => void;
};

export const GlossaryHighlight = ({
  text,
  term,
  languageTag,
  targetLanguageTag,
  onTranslationUpdated,
}: Props) => {
  const { preferredOrganization } = usePreferredOrganization();
  const editEnabled = ['OWNER', 'MAINTAINER'].includes(
    preferredOrganization?.currentUserRole || ''
  );

  return (
    <Tooltip
      placement="bottom-start"
      enterDelay={200}
      components={{ Tooltip: TooltipCard }}
      title={
        <GlossaryTermPreview
          term={term}
          languageTag={languageTag}
          targetLanguageTag={targetLanguageTag}
          editEnabled={editEnabled}
          standalone
          onTranslationUpdated={onTranslationUpdated}
        />
      }
    >
      <StyledHighlight data-cy="glossary-term-highlight">
        {text}
      </StyledHighlight>
    </Tooltip>
  );
};
