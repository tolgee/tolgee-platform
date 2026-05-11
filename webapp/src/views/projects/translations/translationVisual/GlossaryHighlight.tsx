import { alpha, styled, Tooltip } from '@mui/material';
import { TooltipCard } from 'tg.component/common/TooltipCard';
import { GlossaryTermPreview } from 'tg.ee';
import { components } from 'tg.service/apiSchema.generated';
import { stopBubble } from 'tg.fixtures/eventHandler';

type GlossaryTermModel = components['schemas']['GlossaryTermModel'];

const StyledHighlight = styled('span')`
  text-decoration: underline;
  text-decoration-style: dashed;
  text-decoration-thickness: 2px;
  text-underline-offset: ${({ theme }) => theme.spacing(0.5)};
  border-radius: 2px;
  -webkit-box-decoration-break: clone;
  box-decoration-break: clone;
  transition: background-color 0.1s ease-out;
  &:hover {
    background-color: ${({ theme }) => alpha(theme.palette.text.primary, 0.08)};
    transition: background-color 0.1s ease-in;
  }
`;

type Props = {
  text: string;
  term: GlossaryTermModel;
  languageTag: string;
  targetLanguageTag?: string;
};

export const GlossaryHighlight = ({
  text,
  term,
  languageTag,
  targetLanguageTag,
}: Props) => {
  return (
    <Tooltip
      placement="bottom-start"
      enterDelay={200}
      leaveDelay={200}
      components={{ Tooltip: TooltipCard }}
      title={
        <div onClick={stopBubble()}>
          <GlossaryTermPreview
            term={term}
            languageTag={languageTag}
            targetLanguageTag={targetLanguageTag}
            standalone
          />
        </div>
      }
    >
      <StyledHighlight data-cy="glossary-term-highlight">
        {text}
      </StyledHighlight>
    </Tooltip>
  );
};
