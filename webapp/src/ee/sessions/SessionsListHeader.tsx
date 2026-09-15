import { Box, styled } from '@mui/material';
import { T } from '@tolgee/react';

import {
  SESSIONS_GRID_COLUMNS,
  SESSIONS_ROW_PADDING,
  SESSIONS_WIDE_LAYOUT,
} from './sessionsGrid';

const StyledHeaderRow = styled(Box)`
  display: none;

  @container (min-width: ${SESSIONS_WIDE_LAYOUT}) {
    display: grid;
    grid-template-columns: ${SESSIONS_GRID_COLUMNS};
    gap: 8px;
    padding: ${SESSIONS_ROW_PADDING};
    border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
    font-size: 13px;
    font-weight: 600;
    line-height: 1.5;
    color: ${({ theme }) => theme.palette.text.secondary};
  }
`;

export function SessionsListHeader() {
  return (
    <StyledHeaderRow data-cy="sessions-list-header">
      <Box>
        <T keyName="sessions-column-device" defaultValue="Device" />
      </Box>
      <Box>
        <T keyName="sessions-column-location" defaultValue="Location" />
      </Box>
      <Box>
        <T keyName="sessions-column-signed-in" defaultValue="Signed in" />
      </Box>
      <Box>
        <T keyName="sessions-column-last-used" defaultValue="Last used" />
      </Box>
      <Box />
    </StyledHeaderRow>
  );
}
