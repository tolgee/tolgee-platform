import { FilterLines } from '@untitled-ui/icons-react';
import { Box, Button, styled, useMediaQuery } from '@mui/material';
import { T } from '@tolgee/react';

import { usePrefilter } from './usePrefilter';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import React from 'react';

const StyledContainer = styled('div')`
  background: ${({ theme }) => theme.palette.revisionFilterBanner.background};
  padding: 0px 4px 0px 14px;
  border-radius: 4px;
  height: 40px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  max-width: 100%;
  align-items: center;
`;

const StyledLabel = styled('div')`
  color: ${({ theme }) => theme.palette.revisionFilterBanner.highlightText};
  display: flex;
  align-items: center;
  gap: 6px;
  margin-right: 16px;
  flex-shrink: 1;
  overflow: hidden;
`;

const StyledLabelText = styled('div')`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 600;
`;

const StyledClear = styled('div')`
  flex-grow: 1;
  display: flex;
  justify-content: flex-end;
  white-space: nowrap;
`;

type Props = {
  title: React.ReactNode;
  content: React.ReactNode;
  icon?: React.ReactNode;
  closeButton?: React.ReactNode;
  controls?: React.ReactNode;
  alert?: React.ReactNode;
};

export const PrefilterContainer = ({
  title,
  content,
  icon,
  closeButton,
  controls,
  alert,
}: Props) => {
  const prefilter = usePrefilter();

  const rightPanelWidth = useGlobalContext((c) => c.layout.rightPanelWidth);
  const isSmall = useMediaQuery(
    `@media(max-width: ${rightPanelWidth + 1000}px)`
  );

  return (
    <StyledContainer>
      <StyledLabel>
        {icon ?? <FilterLines />}
        <StyledLabelText>{title}</StyledLabelText>
      </StyledLabel>
      <Box display="flex" gap={0.5}>
        {!isSmall && content}
        {controls}
        {!isSmall && alert}
      </Box>
      <StyledClear>
        {closeButton ?? (
          <Button size="small" onClick={prefilter?.clear} color="inherit">
            <T keyName="activity_filter_indicator_clear" />
          </Button>
        )}
      </StyledClear>
    </StyledContainer>
  );
};
