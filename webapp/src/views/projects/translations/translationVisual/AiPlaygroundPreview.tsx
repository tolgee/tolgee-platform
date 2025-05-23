import { Box, styled, SxProps, Tooltip, useTheme } from '@mui/material';
import { Stars } from 'tg.component/CustomIcons';
import { TranslationVisual } from './TranslationVisual';

const StyledAiPreview = styled(Box)`
  display: grid;
  background: ${({ theme }) => theme.palette.tokens.secondary._states.selected};
  grid-template-columns: auto 1fr;
  gap: 4px;
  padding: 8px;
  border-radius: 8px;
`;

const StyledContent = styled('div')`
  display: grid;
`;

type Props = {
  translation: string | undefined;
  tooltip?: React.ReactNode;
  isPlural: boolean;
  locale: string;
  sx?: SxProps;
};

export const AiPlaygroundPreview = ({
  translation,
  tooltip,
  isPlural,
  locale,
  sx,
}: Props) => {
  const theme = useTheme();
  const content = (
    <StyledAiPreview {...{ sx }} data-cy="ai-playground-preview">
      <Stars width={20} height={20} color={theme.palette.secondary.main} />
      <StyledContent>
        <TranslationVisual
          text={translation}
          isPlural={isPlural}
          locale={locale}
          maxLines={3}
          extraPadding={false}
        />
      </StyledContent>
    </StyledAiPreview>
  );
  return tooltip ? <Tooltip title={tooltip}>{content}</Tooltip> : content;
};
