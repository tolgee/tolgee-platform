import { Box, Button, ButtonGroup, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { useEffect, useState } from 'react';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { PermissionsAdvanced } from './PermissionsAdvanced';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import { PermissionsBasic } from './PermissionsBasic';
import {
  LanguageModel,
  PermissionAdvancedState,
  PermissionBasicState,
  PermissionModel,
  PermissionSettingsState,
  RolesMap,
  TabsType,
} from './types';

import { getLanguagesByRole } from './utils';

type Props = {
  title: string;
  permissions: PermissionModel;
  onChange: (state: PermissionSettingsState) => void;
  height?: number;
  allLangs?: LanguageModel[];
  hideNone?: boolean;
  disabled?: boolean;
};

export const PermissionsSettings: React.FC<Props> = ({
  title,
  permissions,
  onChange,
  allLangs,
  hideNone,
  disabled,
}) => {
  const [tab, setTab] = useState<TabsType>(
    permissions.type ? 'basic' : 'advanced'
  );

  const rolesLoadable = useApiQuery({
    url: '/v2/public/scope-info/roles',
    method: 'get',
  });

  const dependenciesLoadable = useApiQuery({
    url: '/v2/public/scope-info/hierarchy',
    method: 'get',
    query: {},
  });

  const [basicState, setBasicState] = useState<PermissionBasicState>({
    role: permissions.type,
    languages: getLanguagesByRole(permissions) || undefined,
  });

  const [advancedState, setAdvancedState] = useState<
    PermissionAdvancedState | undefined
  >(undefined);

  useEffect(() => {
    if (dependenciesLoadable.data && rolesLoadable.data && !advancedState) {
      const scopes =
        (permissions.type
          ? rolesLoadable.data[permissions.type]
          : permissions.scopes) || [];
      setAdvancedState({
        scopes,
        viewLanguages: permissions.viewLanguageIds || [],
        translateLanguages: permissions.translateLanguageIds || [],
        stateChangeLanguages: permissions.stateChangeLanguageIds || [],
        suggestLanguages: permissions.suggestLanguageIds || [],
      });
    }
  }, [dependenciesLoadable.data, rolesLoadable.data, advancedState]);

  useEffect(() => {
    if (advancedState) {
      onChange({
        tab,
        advancedState,
        basicState,
      });
    }
  }, [basicState, advancedState, tab]);

  const handleChange = (tab: TabsType) => () => {
    setTab(tab);
  };

  if (dependenciesLoadable.isLoading || rolesLoadable.isLoading) {
    return <FullPageLoading />;
  }

  if (!dependenciesLoadable.data || !rolesLoadable.data || !advancedState) {
    return null;
  }

  return (
    <Box data-cy="permissions-menu">
      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="flex-start"
        mb={2}
      >
        <Typography variant="h5">{title}</Typography>
        <ButtonGroup size="small" disabled={disabled}>
          <Button
            color={tab === 'basic' ? 'primary' : 'default'}
            onClick={handleChange('basic')}
            data-cy="permissions-menu-basic"
          >
            <T keyName="permission_basic_title" />
          </Button>
          <Button
            color={tab === 'advanced' ? 'primary' : 'default'}
            onClick={handleChange('advanced')}
            data-cy="permissions-menu-granular"
          >
            <T keyName="permission_granular_title" />
          </Button>
        </ButtonGroup>
      </Box>

      <Box sx={{ mt: 1 }} overflow="auto" mx={-1.5}>
        {tab === 'basic' && (
          <PermissionsBasic
            state={basicState}
            onChange={setBasicState}
            roles={rolesLoadable.data as RolesMap}
            allLangs={allLangs}
            hideNone={hideNone}
            disabled={disabled}
          />
        )}
        {tab === 'advanced' && (
          <Box mx={1.5}>
            <PermissionsAdvanced
              state={advancedState}
              onChange={setAdvancedState}
              dependencies={dependenciesLoadable.data}
              allLangs={allLangs}
            />
          </Box>
        )}
      </Box>
    </Box>
  );
};
