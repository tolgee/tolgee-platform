import { T } from '@tolgee/react';
import {
  Dialog,
  DialogContent,
  DialogTitle,
  Box,
  Tabs,
  Tab,
} from '@mui/material';
import { useState } from 'react';
import { PermissionsBasic } from './PermissionsBasic';
import { PermissionBasic, PermissionModel } from './types';
import { PermissionsAdvanced } from './PermissionsAdvanced';

type Tabs = 'basic' | 'advanced';

type Props = {
  onClose: () => void;
  nameInTitle?: string;
  permissions: PermissionModel;
};

export const PermissionsModal: React.FC<Props> = ({
  onClose,
  nameInTitle,
  permissions,
}) => {
  const [tab, setTab] = useState<Tabs>('basic');

  const handleChange = (_: React.SyntheticEvent, newValue: Tabs) => {
    setTab(newValue);
  };

  const [basic, setBasic] = useState<PermissionBasic>({
    role: permissions.type,
    viewLanguages: permissions.viewLanguageIds,
    languages: permissions.permittedLanguageIds,
    stateChangeLanguages: permissions.stateChangeLanguageIds,
  });

  return (
    <Dialog open={true} onClose={onClose} fullWidth>
      <DialogTitle>
        <T keyName="permission_dialog_title" params={{ name: nameInTitle }} />
      </DialogTitle>
      <DialogContent sx={{ minHeight: 400 }}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs
            value={tab}
            onChange={handleChange}
            aria-label="basic tabs example"
          >
            <Tab
              label={<T keyName="permission_basic_title" />}
              value={'basic'}
            />
            <Tab
              label={<T keyName="permission_advanced_title" />}
              value={'advanced'}
            />
          </Tabs>
        </Box>
        <Box sx={{ mt: 1 }}>
          {tab === 'basic' && (
            <PermissionsBasic
              value={basic}
              onChange={(value) => setBasic(value)}
            />
          )}
          {tab === 'advanced' && <PermissionsAdvanced />}
        </Box>
      </DialogContent>
    </Dialog>
  );
};
