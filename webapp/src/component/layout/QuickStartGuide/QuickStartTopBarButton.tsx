import { Box, Button, styled } from '@mui/material';
import { RocketIcon } from 'tg.component/CustomIcons';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';
import { items } from './quickStartConfig';
import { QuickStartProgress } from './QuickStartProgress';

const StyledContainer = styled(Box)`
  position: relative;
`;

const StyledButton = styled(Button)`
  min-width: 0px;
  margin-right: 2px;
`;

export const QuickStartTopBarButton = () => {
  const guideEnabled = useGlobalContext((c) => c.quickStartGuide.enabled);
  const guideOpen = useGlobalContext((c) => c.quickStartGuide.open);
  const completedSteps = useGlobalContext(
    (c) => c.quickStartGuide.completed.length
  );
  const allSteps = items.length;
  const { setQuickStartOpen } = useGlobalActions();

  return (
    <>
      {guideEnabled && (
        <StyledContainer>
          <StyledButton
            onClick={() => setQuickStartOpen(!guideOpen)}
            color="inherit"
          >
            <Box display="flex" gap={1} alignItems="center">
              <RocketIcon fontSize="small" />
              <QuickStartProgress percent={completedSteps / allSteps} />
            </Box>
          </StyledButton>
        </StyledContainer>
      )}
    </>
  );
};
