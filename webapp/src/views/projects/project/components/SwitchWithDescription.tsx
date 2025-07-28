import { Box, styled, Switch } from '@mui/material';
import React from 'react';

const StyledContainer = styled('div')`
  display: grid;
  grid-template-areas:
    'title       switch'
    'description switch';
  grid-template-columns: 1fr auto;
`;

const StyledTitle = styled(Box)`
  font-size: 16px;
  padding: 4px 0px;
`;

const StyledDescription = styled(Box)`
  font-size: 14px;
`;

type Props = {
  title: React.ReactNode;
  description: React.ReactNode;
  onSwitch: () => void;
  checked: boolean;
  disabled: boolean;
  'data-cy'?: string;
};

export const SwitchWithDescription = ({
  title,
  description,
  onSwitch,
  checked,
  disabled,
  'data-cy': dataCy,
}: Props) => {
  return (
    <StyledContainer>
      <StyledTitle gridArea="title">{title}</StyledTitle>
      <StyledDescription gridArea="description">
        {description}
      </StyledDescription>
      <Box gridArea="switch">
        <Switch
          checked={checked}
          onChange={disabled ? undefined : onSwitch}
          data-cy={dataCy}
        />
      </Box>
    </StyledContainer>
  );
};
