import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  Box,
  Tooltip,
  styled,
} from '@mui/material';

import {
  LanguageModel,
  PermissionModel,
  PermissionSettingsState,
} from 'tg.component/PermissionsSettings/types';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { PermissionsSettings } from 'tg.component/PermissionsSettings/PermissionsSettings';
import { useMessage } from 'tg.hooks/useSuccessMessage';

const StyledBanner = styled('div')`
  background: ${({ theme }) => theme.palette.warning.light};
  color: ${({ theme }) => theme.palette.grey[900]};
  padding: ${({ theme }) => theme.spacing(0.5, 3)};
`;

export type PermissionModalProps = {
  allLangs?: LanguageModel[];
  onClose: () => void;
  title: string;
  permissions: PermissionModel;
  onSubmit: (settings: PermissionSettingsState) => Promise<void>;
  isInheritedFromOrganization?: boolean;
  onResetToOrganization?: () => Promise<void>;
};

export const PermissionsModal: React.FC<PermissionModalProps> = ({
  allLangs,
  onClose,
  title,
  permissions,
  onSubmit,
  isInheritedFromOrganization,
  onResetToOrganization,
}) => {
  const messages = useMessage();
  const [saveLoading, setSaveLoading] = useState(false);
  const [resetLoading, setResetLoading] = useState(false);
  const { t } = useTranslate();

  const [settingsState, setSettingsState] = useState<
    PermissionSettingsState | undefined
  >(undefined);

  const handleUpdatePermissions = async () => {
    if (settingsState) {
      if (
        settingsState.tab === 'advanced' &&
        settingsState.advancedState.scopes.length === 0
      ) {
        messages.error(<T keyName="scopes_at_least_one_scope_error" />);
        return;
      }
      setSaveLoading(true);
      onSubmit(settingsState)
        .then(() => {
          onClose();
        })
        .finally(() => {
          setSaveLoading(false);
        });
    }
  };

  const handleReset = async () => {
    setResetLoading(true);
    onResetToOrganization?.()
      .then(() => {
        onClose();
      })
      .finally(() => {
        setResetLoading(false);
      });
  };

  return (
    <Dialog open={true} onClose={onClose} fullWidth>
      {isInheritedFromOrganization && (
        <StyledBanner data-cy="permissions-menu-inherited-message">
          {t('permission_dialog_inherited_message')}
        </StyledBanner>
      )}
      <DialogContent sx={{ height: 'min(80vh, 530px)' }}>
        <PermissionsSettings
          title={title}
          permissions={permissions}
          onChange={setSettingsState}
          allLangs={allLangs}
        />
      </DialogContent>
      <Box display="flex" justifyContent="space-between">
        <DialogActions>
          {onResetToOrganization && (
            <Tooltip title={t('permission_dialog_reset_to_organization_hint')}>
              <div>
                <LoadingButton
                  loading={resetLoading}
                  onClick={handleReset}
                  data-cy="permissions-menu-reset-to-organization"
                  variant="outlined"
                  color="secondary"
                >
                  <T keyName="permission_dialog_reset_to_organization" />
                </LoadingButton>
              </div>
            </Tooltip>
          )}
        </DialogActions>
        <DialogActions>
          <Button onClick={onClose} data-cy="permissions-menu-close">
            <T keyName="permission_dialog_close" />
          </Button>
          <LoadingButton
            loading={saveLoading}
            onClick={handleUpdatePermissions}
            color="primary"
            variant="contained"
            data-cy="permissions-menu-save"
          >
            <T keyName="permission_dialog_save" />
          </LoadingButton>
        </DialogActions>
      </Box>
    </Dialog>
  );
};
