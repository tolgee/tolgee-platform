import React from 'react';
import { styled, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { TabMessage } from './TabMessage';
import { UseQueryResult } from 'react-query';

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
  background: ${({ theme }) => theme.palette.extraLightBackground.main};
  border-bottom: 1px solid
    ${({ theme }) => theme.palette.extraLightDivider.main};
  text-transform: uppercase;
  color: #808080;
  position: sticky;
  top: 0px;
  height: 32px;
  flex-shrink: 1;
  flex-basis: 0px;
`;

const StyledBadge = styled('div')`
  background: ${({ theme }) => theme.palette.lightBackground.main};
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
  data?: UseQueryResult<unknown, any>;
};

export const ToolsTab: React.FC<Props> = ({
  icon,
  title,
  badgeNumber,
  children,
  data,
}) => {
  const t = useTranslate();

  const getErrorMessage = (code: string) => {
    switch (code) {
      case 'out_of_credits':
        return t('translation_tools_no_credits');
      default:
        return code;
    }
  };

  const error = data?.error;
  const errorCode = error?.message || error?.code || error || 'Unknown error';

  const errorMessage = getErrorMessage(errorCode);

  return (
    <StyledContainer>
      <StyledTab>
        {icon}
        <StyledTitle variant="button">{title}</StyledTitle>
        {badgeNumber ? <StyledBadge>{badgeNumber}</StyledBadge> : null}
      </StyledTab>

      {data?.isError ? (
        <TabMessage type="error" message={errorMessage} />
      ) : (
        children
      )}
    </StyledContainer>
  );
};
