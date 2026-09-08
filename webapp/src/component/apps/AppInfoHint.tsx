import React from 'react';
import { Tooltip, styled } from '@mui/material';
import { InfoCircle } from '@untitled-ui/icons-react';

const StyledIcon = styled('span')`
  display: inline-flex;
  align-items: center;
  color: ${({ theme }) => theme.palette.text.secondary};
  cursor: help;
  & svg {
    width: 15px;
    height: 15px;
  }
`;

const StyledGrid = styled('div')`
  display: grid;
  grid-template-columns: auto 1fr;
  gap: ${({ theme }) => theme.spacing(0.25, 1.5)};
  padding: ${({ theme }) => theme.spacing(0.5)};
`;

const StyledLabel = styled('span')`
  opacity: 0.7;
  white-space: nowrap;
`;

const StyledValue = styled('span')`
  font-family: monospace;
  word-break: break-all;
`;

export type AppInfoEntry = {
  label: React.ReactNode;
  value: React.ReactNode;
};

/** A small "i" revealing the app facts that are too noisy for the list row itself. */
export const AppInfoHint = ({
  entries,
  dataCy,
}: {
  entries: AppInfoEntry[];
  dataCy: string;
}) => (
  <Tooltip
    slotProps={{ tooltip: { sx: { maxWidth: 480 } } }}
    title={
      <StyledGrid>
        {entries.map((entry, index) => (
          <React.Fragment key={index}>
            <StyledLabel>{entry.label}</StyledLabel>
            <StyledValue>{entry.value}</StyledValue>
          </React.Fragment>
        ))}
      </StyledGrid>
    }
  >
    <StyledIcon tabIndex={0} data-cy={dataCy}>
      <InfoCircle />
    </StyledIcon>
  </Tooltip>
);
