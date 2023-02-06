import { Box } from '@mui/material';

import { RoleLanguage } from './RoleLanguage';
import { HierarchyItem, PermissionState } from './types';

type Props = {
  state: PermissionState;
  onChange: (value: PermissionState) => void;
  dependencies: HierarchyItem;
};

export const RoleLanguages: React.FC<Props> = ({
  state,
  onChange,
  dependencies,
}) => {
  const scopes = state.scopes;
  return (
    <Box display="flex" gap={1}>
      {scopes.includes('translations.view') && (
        <RoleLanguage
          scope="translations.view"
          state={state}
          onChange={onChange}
          dependencies={dependencies}
        />
      )}

      {scopes.includes('translations.edit') && (
        <RoleLanguage
          scope="translations.edit"
          state={state}
          onChange={onChange}
          dependencies={dependencies}
        />
      )}

      {scopes.includes('translations.state-edit') && (
        <RoleLanguage
          scope="translations.state-edit"
          state={state}
          onChange={onChange}
          dependencies={dependencies}
        />
      )}
    </Box>
  );
};
