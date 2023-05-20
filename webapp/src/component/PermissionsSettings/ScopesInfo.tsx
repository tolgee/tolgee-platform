import { Info } from '@mui/icons-material';
import { styled, Tooltip } from '@mui/material';

import { ScopesHint } from './ScopesHint';
import { PermissionModelScope } from './types';

const StyledInfo = styled(Info)`
  opacity: 0.5;
`;

type Props = {
  scopes: PermissionModelScope[];
};

export function ScopesInfo({ scopes }: Props) {
  return (
    <Tooltip title={<ScopesHint scopes={scopes} />}>
      <StyledInfo fontSize="small" color="inherit" />
    </Tooltip>
  );
}
