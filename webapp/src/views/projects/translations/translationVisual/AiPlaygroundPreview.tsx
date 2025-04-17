import { styled, Tooltip, useTheme } from '@mui/material';
import { Stars } from 'tg.component/CustomIcons';
import { TranslationVisual } from './TranslationVisual';

const StyledAiPreview = styled('div')`
  display: grid;
  background: ${({ theme }) => theme.palette.tokens.secondary._states.selected};
  justify-self: start;
  align-self: start;
  grid-template-columns: auto 1fr;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 8px;
`;

const StyledContent = styled('div')`
  display: grid;
`;

type Props = {
  translation: string | undefined;
  contextDescription: string | undefined;
  isPlural: boolean;
  locale: string;
};

export const AiPlaygroundPreview = ({
  translation,
  contextDescription,
  isPlural,
  locale,
}: Props) => {
  const theme = useTheme();
  return (
    <Tooltip title={contextDescription}>
      <StyledAiPreview>
        <Stars width={20} height={20} color={theme.palette.secondary.main} />
        <StyledContent>
          <TranslationVisual
            text={translation}
            isPlural={isPlural}
            locale={locale}
            maxLines={3}
          />
        </StyledContent>
      </StyledAiPreview>
    </Tooltip>
  );
};
