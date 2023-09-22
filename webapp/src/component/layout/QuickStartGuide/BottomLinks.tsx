import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { StyledLink } from './StyledComponents';

const StyledContainer = styled(Box)`
  background: ${({ theme }) => theme.palette.emphasis[50]};
  padding: 10px 18px;
`;

type Props = {
  allCompleted: boolean;
};

export const BottomLinks = ({ allCompleted }: Props) => {
  const { t } = useTranslate();
  const { quickStartFinish } = useGlobalActions();

  return (
    <StyledContainer>
      <Box display="flex" justifyContent="space-between">
        <Box display="flex" gap={2}>
          <StyledLink
            href="https://tolgee.io/platform"
            target="_blank"
            rel="noreferrer noopener"
          >
            {t('guide_links_docs_platform')}
          </StyledLink>
        </Box>
        {!allCompleted && (
          <StyledLink className="secondary" onClick={() => quickStartFinish()}>
            {t('guide_links_skip')}
          </StyledLink>
        )}
      </Box>
    </StyledContainer>
  );
};
