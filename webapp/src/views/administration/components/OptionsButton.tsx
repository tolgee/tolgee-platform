import { useState } from 'react';
import { DotsVertical } from '@untitled-ui/icons-react';
import { IconButton, Menu, MenuItem } from '@mui/material';
import { useTranslate, T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useUser } from 'tg.globalContext/helpers';
import { confirmation } from 'tg.hooks/confirmation';

type UserAccountModel = components['schemas']['UserAccountModel'];

type Props = {
  user: UserAccountModel;
};

export function OptionsButton({ user }: Props) {
  const { t } = useTranslate();
  const [anchor, setAnchor] = useState<HTMLElement>();
  const message = useMessage();
  const loggedInUser = useUser();

  const deleteUser = useApiMutation({
    url: '/v2/administration/users/{userId}',
    method: 'delete',
    invalidatePrefix: '/v2/administration',
    options: {
      onSuccess() {
        message.success(<T keyName="administration_user_deleted_message" />);
      },
    },
  });

  const disableUser = useApiMutation({
    url: '/v2/administration/users/{userId}/disable',
    method: 'put',
    invalidatePrefix: '/v2/administration',
    options: {
      onSuccess() {
        setAnchor(undefined);
        message.success(<T keyName="administration_user_disabled_message" />);
      },
    },
  });

  const enableUser = useApiMutation({
    url: '/v2/administration/users/{userId}/enable',
    method: 'put',
    invalidatePrefix: '/v2/administration',
    options: {
      onSuccess() {
        setAnchor(undefined);
        message.success(<T keyName="administration_user_enabled_message" />);
      },
    },
  });

  function handleDeleteUser() {
    confirmation({
      message: t('delete-user-confirmation-message', {
        name: user.name || user.username,
      }),
      onConfirm() {
        deleteUser.mutate({ path: { userId: user.id } });
      },
    });
  }

  function handleDisableUser() {
    confirmation({
      message: (
        <T
          keyName="disable-user-confirmation-message"
          params={{ name: user.name || user.username }}
        />
      ),
      onConfirm() {
        disableUser.mutate({ path: { userId: user.id } });
      },
    });
  }

  function handleEnableUser() {
    enableUser.mutate({ path: { userId: user.id } });
  }

  const isLoading =
    enableUser.isLoading || disableUser.isLoading || deleteUser.isLoading;

  return (
    <>
      <IconButton
        onClick={(e) => setAnchor(e.target as HTMLElement)}
        data-cy="administration-user-menu"
      >
        <DotsVertical />
      </IconButton>
      {anchor && (
        <Menu
          open={true}
          anchorEl={anchor}
          onClose={() => setAnchor(undefined)}
        >
          <MenuItem
            data-cy="administration-user-delete-user"
            disabled={loggedInUser?.id === user.id || isLoading}
            onClick={handleDeleteUser}
          >
            {t('administration_delete_user_button')}
          </MenuItem>
          {user.disabled ? (
            <MenuItem
              data-cy="administration-user-enable-user"
              disabled={loggedInUser?.id === user.id || isLoading}
              onClick={handleEnableUser}
            >
              {t('administration_enable_user_button')}
            </MenuItem>
          ) : (
            <MenuItem
              data-cy="administration-user-disable-user"
              disabled={loggedInUser?.id === user.id || isLoading}
              onClick={handleDisableUser}
            >
              {t('administration_disable_user_button')}
            </MenuItem>
          )}
        </Menu>
      )}
    </>
  );
}
