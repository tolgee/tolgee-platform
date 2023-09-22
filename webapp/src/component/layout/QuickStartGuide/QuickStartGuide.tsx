import { Box, Button, styled, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useMemo } from 'react';
import { RocketIcon } from 'tg.component/CustomIcons';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';
import { BottomLinks } from './BottomLinks';
import { items } from './quickStartConfig';
import { QuickStartStep } from './QuickStartStep';

const StyledContainer = styled(Box)`
  display: grid;
  gap: 8px;
  grid-template-rows: auto 1fr auto;
  height: 100%;
  position: relative;
  border-top: 1px solid ${({ theme }) => theme.palette.quickStart.topBorder};
`;

const StyledContent = styled(Box)`
  display: grid;
  gap: 8px;
  align-self: start;
`;

const StyledHeader = styled(Box)`
  display: flex;
  background: ${({ theme }) => theme.palette.emphasis[50]};
  border-radius: 0px 0px 16px 16px;
  font-size: 23px;
  font-weight: 400;
  padding: 13px 23px;
  align-items: center;
  gap: 12px;
`;

const StyledArrow = styled(Box)`
  position: absolute;
  top: -10px;
  right: 170px;
  width: 0;
  height: 0;
  border-left: 10px solid transparent;
  border-right: 10px solid transparent;
  border-bottom: 10px solid ${({ theme }) => theme.palette.emphasis[50]};
  transition: opacity 0.2s ease-in-out;
`;

export const QuickStartGuide = () => {
  const { t } = useTranslate();
  const projectId = useGlobalContext((c) => c.quickStartGuide.lastProjectId);
  const completed = useGlobalContext((c) => c.quickStartGuide.completed);
  const { quickStartFinish } = useGlobalActions();
  const topBarHeight = useGlobalContext((c) => c.topBarHeight);
  const allCompleted = useMemo(
    () => items.every((i) => completed.includes(i.step)),
    [completed, items]
  );

  return (
    <StyledContainer data-cy="quick-start-dialog">
      <StyledArrow sx={{ opacity: topBarHeight ? 1 : 0 }} />
      <StyledHeader>
        <RocketIcon fontSize="small" />
        <T keyName="guide_title" />
      </StyledHeader>
      <StyledContent>
        {items.map((item, i) => (
          <QuickStartStep
            key={i}
            index={i + 1}
            item={item}
            projectId={projectId}
            done={completed.includes(item.step)}
          />
        ))}
        {allCompleted && (
          <Box sx={{ display: 'grid', justifyItems: 'center', gap: 2, pt: 1 }}>
            <Typography>{t('guide_finish_text')}</Typography>
            <Button
              data-cy="quick-start-finish"
              color="primary"
              variant="contained"
              onClick={() => quickStartFinish()}
            >
              {t('guide_finish_button')}
            </Button>
          </Box>
        )}
      </StyledContent>
      <BottomLinks allCompleted={allCompleted} />
    </StyledContainer>
  );
};
