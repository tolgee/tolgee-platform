import { Box, styled } from '@mui/material';
import { T } from '@tolgee/react';

import {
  CONNECTED_APPS_GRID_COLUMNS,
  CONNECTED_APPS_ROW_PADDING,
  CONNECTED_APPS_WIDE_LAYOUT,
} from './connectedAppsGrid';

const StyledHeaderRow = styled(Box)`
  display: none;

  @container (min-width: ${CONNECTED_APPS_WIDE_LAYOUT}) {
    display: grid;
    grid-template-columns: ${CONNECTED_APPS_GRID_COLUMNS};
    gap: 8px;
    padding: ${CONNECTED_APPS_ROW_PADDING};
    border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
    font-size: 13px;
    font-weight: 600;
    line-height: 1.5;
    color: ${({ theme }) => theme.palette.text.secondary};
  }
`;

export function ConnectedAppsListHeader() {
  return (
    <StyledHeaderRow data-cy="connected-apps-list-header">
      <Box>
        <T keyName="connected-apps-column-app" defaultValue="App" />
      </Box>
      <Box>
        <T keyName="connected-apps-column-access" defaultValue="Access" />
      </Box>
      <Box>
        <T keyName="connected-apps-column-projects" defaultValue="Projects" />
      </Box>
      <Box>
        <T
          keyName="connected-apps-column-authorized"
          defaultValue="Authorized"
        />
      </Box>
      <Box>
        <T
          keyName="connected-apps-column-last-active"
          defaultValue="Last active"
        />
      </Box>
      <Box />
    </StyledHeaderRow>
  );
}
