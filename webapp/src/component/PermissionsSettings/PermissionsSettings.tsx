import { Box, Tab, Tabs } from '@mui/material';
import { T } from '@tolgee/react';
import { useEffect, useState } from 'react';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { updateByDependencies } from 'tg.ee/PermissionsAdvanced/hierarchyTools';
import { PermissionsAdvanced } from 'tg.ee/PermissionsAdvanced/PermissionsAdvanced';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import { PermissionsBasic } from './PermissionsBasic';
import {
  PermissionState,
  PermissionModel,
  PermissionSettingsState,
  RolesMap,
  TabsType,
} from './types';

type Props = {
  permissions: PermissionModel;
  onChange: (state: PermissionSettingsState) => void;
};

export const PermissionsSettings: React.FC<Props> = ({
  permissions,
  onChange,
}) => {
  const allLangs = useProjectLanguages().map((l) => l.id);
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

  const [state, setState] = useState<PermissionState | undefined>(undefined);

  useEffect(() => {
    if (dependenciesLoadable.data && !state) {
      setState(
        updateByDependencies(
          permissions.scopes,
          {
            role: permissions.type,
            scopes: permissions.scopes,
            viewLanguages: permissions.viewLanguageIds || [],
            translateLanguages: permissions.translateLanguageIds || [],
            stateChangeLanguages: permissions.stateChangeLanguageIds || [],
          },
          dependenciesLoadable.data,
          allLangs
        )
      );
    }
  }, [dependenciesLoadable.data]);

  useEffect(() => {
    if (state) {
      onChange({
        tab,
        state,
      });
    }
  }, [tab, state]);

  const handleChange = (_: React.SyntheticEvent, newValue: TabsType) => {
    setTab(newValue);
  };

  if (dependenciesLoadable.isLoading || rolesLoadable.isLoading) {
    return <FullPageLoading />;
  }

  if (!dependenciesLoadable.data || !rolesLoadable.data || !state) {
    return null;
  }

  return (
    <div>
      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs
          value={tab}
          onChange={handleChange}
          aria-label="basic tabs example"
        >
          <Tab label={<T keyName="permission_basic_title" />} value={'basic'} />
          <Tab
            label={<T keyName="permission_advanced_title" />}
            value={'advanced'}
          />
        </Tabs>
      </Box>
      <Box sx={{ mt: 1 }}>
        {tab === 'basic' && (
          <PermissionsBasic
            state={state}
            onChange={setState}
            roles={rolesLoadable.data as RolesMap}
            dependencies={dependenciesLoadable.data}
          />
        )}
        {tab === 'advanced' && (
          <PermissionsAdvanced
            state={state}
            onChange={setState}
            dependencies={dependenciesLoadable.data}
          />
        )}
      </Box>
    </div>
  );
};
