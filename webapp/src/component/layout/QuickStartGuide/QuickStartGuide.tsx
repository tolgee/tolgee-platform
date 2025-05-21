import { Box, IconButton, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { useMemo } from 'react';
import { X } from '@untitled-ui/icons-react';

import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { RocketFilled } from 'tg.component/CustomIcons';
import { BottomLinks } from './BottomLinks';
import { items } from './quickStartConfig';
import { QuickStartStep } from './QuickStartStep';
import { QuickStartFinishStep } from './QuickStartFinishStep';

const StyledContainer = styled(Box)`
  display: grid;
  gap: 8px;
  grid-template-rows: auto 0px 1fr auto;
  height: 100%;
  position: relative;
`;

const StyledContent = styled(Box)`
  display: grid;
  gap: 8px;
  align-self: start;
  overflow: auto;
  max-height: 100%;
`;

const StyledHeader = styled(Box)`
  display: flex;
  background: ${({ theme }) => theme.palette.quickStart.highlight};
  border-radius: 0px 0px 16px 16px;
  font-size: 23px;
  font-weight: 400;
  padding: 13px 23px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
`;

const StyledArrow = styled(Box)`
  position: absolute;
  top: -10px;
  right: 87px;
  width: 0;
  height: 0;
  border-left: 10px solid transparent;
  border-right: 10px solid transparent;
  border-bottom: 10px solid ${({ theme }) => theme.palette.quickStart.highlight};
  transition: opacity 0.2s ease-in-out;
`;

type Props = {
  onClose: () => void;
};

export const QuickStartGuide = ({ onClose }: Props) => {
  const projectId = useGlobalContext((c) => c.quickStartGuide.lastProjectId);
  const completed = useGlobalContext((c) => c.quickStartGuide.completed);
  const topBarHeight = useGlobalContext((c) => c.layout.topBarHeight);
  const allCompleted = useMemo(
    () => items.every((i) => completed.includes(i.step)),
    [completed, items]
  );

  return (
    <StyledContainer data-cy="quick-start-dialog">
      <StyledArrow sx={{ opacity: topBarHeight ? 1 : 0 }} />
      <StyledHeader>
        <Box display="flex" gap="12px" alignItems="center">
          <RocketFilled width={20} height={20} />
          <T keyName="guide_title" />
        </Box>
        <IconButton onClick={() => onClose()}>
          <X />
        </IconButton>
      </StyledHeader>
      <Box />
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
        {allCompleted && <QuickStartFinishStep />}
      </StyledContent>
      <BottomLinks allCompleted={allCompleted} />
    </StyledContainer>
  );
};
