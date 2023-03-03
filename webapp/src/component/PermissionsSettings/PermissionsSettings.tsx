import { Box, Button, ButtonGroup } from '@mui/material';
import { T } from '@tolgee/react';
import { useEffect, useState } from 'react';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { updateByDependencies } from 'tg.ee/PermissionsAdvanced/hierarchyTools';
import { PermissionsAdvanced } from 'tg.ee/PermissionsAdvanced/PermissionsAdvanced';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import { PermissionsBasic } from './PermissionsBasic';
import {
  PermissionAdvancedState,
  PermissionBasicState,
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

  const [basicState, setBasicState] = useState<PermissionBasicState>({
    role: permissions.type,
    languages: permissions.stateChangeLanguageIds?.length
      ? permissions.stateChangeLanguageIds
      : permissions.translateLanguageIds,
  });

  const [advancedState, setAdvancedState] = useState<
    PermissionAdvancedState | undefined
  >(undefined);

  useEffect(() => {
    if (dependenciesLoadable.data && !advancedState) {
      setAdvancedState(
        updateByDependencies(
          permissions.scopes,
          {
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
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'end' }}>
        <ButtonGroup size="small">
          <Button
            color={tab === 'basic' ? 'primary' : 'default'}
            onClick={handleChange('basic')}
          >
            <T keyName="permission_basic_title" />
          </Button>
          <Button
            color={tab === 'advanced' ? 'primary' : 'default'}
            onClick={handleChange('advanced')}
          >
            <T keyName="permission_advanced_title" />
          </Button>
        </ButtonGroup>
      </Box>
      <Box sx={{ mt: 1 }}>
        {tab === 'basic' && (
          <PermissionsBasic
            state={basicState}
            onChange={setBasicState}
            roles={rolesLoadable.data as RolesMap}
            dependencies={dependenciesLoadable.data}
          />
        )}
        {tab === 'advanced' && (
          <PermissionsAdvanced
            state={advancedState}
            onChange={setAdvancedState}
            dependencies={dependenciesLoadable.data}
          />
        )}
      </Box>
    </Box>
  );
};
