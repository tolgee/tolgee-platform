import { Box, styled, useTheme } from '@mui/material';
import { StyledLink } from './StyledComponents';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { Tada } from 'tg.component/CustomIcons';
import { useTranslate } from '@tolgee/react';

const StyledContainer = styled(Box)`
  display: flex;
  margin: 0px 8px;
  border-radius: 8px;
  padding: 10px 8px;
  border: 1px solid transparent;
  background: ${({ theme }) => theme.palette.quickStart.finishBackground};
  gap: 8px;
  align-items: center;
`;

const StyledIndex = styled(Box)`
  display: flex;
  width: 30px;
  height: 30px;
  margin: 0px 3px;
  align-items: center;
  justify-content: center;
  background: ${({ theme }) => theme.palette.error.main};
  border-radius: 50%;
  font-size: 14px;
`;

export const QuickStartFinishStep = () => {
  const { t } = useTranslate();
  const { quickStartFinish } = useGlobalActions();
  const theme = useTheme();

  return (
    <StyledContainer data-cy="quick-start-finish-step">
      <StyledIndex>
        <Tada
          width={20}
          height={20}
          color={theme.palette.quickStart.finishIcon}
        />
      </StyledIndex>
      <Box display="grid">
        <Box sx={{ fontWeight: 600 }}>{t('guide_finish_text')}</Box>
        <Box display="flex" gap={2}>
          <StyledLink
            data-cy="quick-start-finish-action"
            onClick={() => quickStartFinish()}
          >
            {t('guide_finish_button')}
          </StyledLink>
        </Box>
      </Box>
    </StyledContainer>
  );
};
