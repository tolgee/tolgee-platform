import { Box, Button, styled } from '@mui/material';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';
import { RocketFilled } from 'tg.component/CustomIcons';
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
  const guideFloatingOpen = useGlobalContext(
    (c) => c.quickStartGuide.floatingOpen
  );
  const quickStartFloating = useGlobalContext(
    (c) => c.layout.quickStartFloating
  );
  const completedSteps = useGlobalContext(
    (c) => c.quickStartGuide.completed.length
  );
  const allSteps = items.length;
  const { setQuickStartOpen, setQuickStartFloatingOpen } = useGlobalActions();

  const handleClick = () => {
    quickStartFloating
      ? setQuickStartFloatingOpen(!guideFloatingOpen)
      : setQuickStartOpen(!guideOpen);
  };

  return (
    <>
      {guideEnabled && (
        <StyledContainer>
          <StyledButton onClick={handleClick} color="inherit">
            <Box display="flex" gap={1} alignItems="center">
              <RocketFilled width={20} height={20} />
              <QuickStartProgress percent={completedSteps / allSteps} />
            </Box>
          </StyledButton>
        </StyledContainer>
      )}
    </>
  );
};
