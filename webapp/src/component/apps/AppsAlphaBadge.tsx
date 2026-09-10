import { Alert, Tooltip, styled } from '@mui/material';
import { T } from '@tolgee/react';

const StyledChip = styled('span')`
  display: inline-flex;
  align-items: center;
  border: 1px solid ${({ theme }) => theme.palette.warning.main};
  color: ${({ theme }) => theme.palette.warning.main};
  border-radius: 999px;
  padding: 0 6px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  line-height: 1.6;
  cursor: help;
`;

const alphaExplanation = (
  <T
    keyName="apps_alpha_banner"
    defaultValue="Apps are in alpha. The behavior and the API can still change significantly."
  />
);

export const AppsAlphaChip = () => (
  <Tooltip title={alphaExplanation}>
    <StyledChip data-cy="apps-alpha-chip">
      <T keyName="apps_alpha_chip" defaultValue="Alpha" />
    </StyledChip>
  </Tooltip>
);

export const AppsAlphaBanner = () => (
  <Alert severity="info" data-cy="apps-alpha-banner">
    {alphaExplanation}
  </Alert>
);
