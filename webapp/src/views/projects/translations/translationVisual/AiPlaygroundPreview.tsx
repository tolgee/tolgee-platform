import { Box, styled, SxProps, Tooltip, useTheme } from '@mui/material';
import { Stars } from 'tg.component/CustomIcons';

const StyledAiPreview = styled(Box)`
  display: grid;
  background: ${({ theme }) => theme.palette.tokens.secondary._states.selected};
  grid-template-columns: auto 1fr;
  gap: 8px;
  padding: 8px;
  border-radius: 8px;
`;

const StyledContent = styled('div')`
  display: grid;
  padding: 2px 0px;
`;

type Props = {
  tooltip?: React.ReactNode;
  sx?: SxProps;
  children: React.ReactNode;
};

export const AiPlaygroundPreview = ({ tooltip, sx, children }: Props) => {
  const theme = useTheme();
  const content = (
    <StyledAiPreview {...{ sx }} data-cy="ai-playground-preview">
      <Stars width={24} height={24} color={theme.palette.secondary.main} />
      <StyledContent>{children}</StyledContent>
    </StyledAiPreview>
  );
  return tooltip ? <Tooltip title={tooltip}>{content}</Tooltip> : content;
};
