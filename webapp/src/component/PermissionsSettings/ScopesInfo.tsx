import { InfoCircle } from '@untitled-ui/icons-react';
import { Box, styled, Tooltip } from '@mui/material';

import { ScopesHint } from './ScopesHint';
import { PermissionModelScope } from './types';

const StyledInfo = styled(InfoCircle)`
  opacity: 0.5;
  width: 22px;
  height: 22px;
`;

type Props = {
  scopes: PermissionModelScope[];
};

export function ScopesInfo({ scopes }: Props) {
  return (
    <Tooltip title={<ScopesHint scopes={scopes} />}>
      <Box display="flex">
        <StyledInfo />
      </Box>
    </Tooltip>
  );
}
