import React from 'react';
import { styled, Typography } from '@mui/material';
import { TabMessage } from './TabMessage';
import { NoCreditsHint } from 'tg.component/NoCreditsHint';

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
  min-width: 0px;
`;

const StyledTab = styled('div')`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
  padding: ${({ theme }) => theme.spacing(0.5, 1)};
  background: ${({ theme }) => theme.palette.cell.selected};
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
  text-transform: uppercase;
  color: ${({ theme }) => theme.palette.text.secondary};
  position: sticky;
  top: 0px;
  height: 32px;
  flex-shrink: 1;
  flex-basis: 0px;
`;

const StyledBadge = styled('div')`
  background: ${({ theme }) => theme.palette.cell.inside};
  padding: 2px 4px;
  border-radius: 12px;
  font-size: 12px;
  height: 20px;
  min-width: 20px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
`;

const StyledTitle = styled(Typography)`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
`;

type Props = {
  icon: React.ReactNode;
  title: string;
  badgeNumber?: number;
  error?: any;
};

export const ToolsTab: React.FC<Props> = ({
  icon,
  title,
  badgeNumber,
  children,
  error,
}) => {
  const errorCode = error?.message || error?.code || error;

  return (
    <StyledContainer>
      <StyledTab>
        {icon}
        <StyledTitle variant="button">{title}</StyledTitle>
        {badgeNumber ? <StyledBadge>{badgeNumber}</StyledBadge> : null}
      </StyledTab>

      {error ? (
        <TabMessage>
          <NoCreditsHint code={errorCode} />
        </TabMessage>
      ) : (
        children
      )}
    </StyledContainer>
  );
};
