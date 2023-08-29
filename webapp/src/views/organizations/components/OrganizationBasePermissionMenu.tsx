import { FunctionComponent } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { container } from 'tsyringe';

import { PermissionsMenu } from 'tg.component/PermissionsSettings/PermissionsMenu';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useUpdateBasePermissions } from './useUpdateBasePermissions';
import { PermissionSettingsState } from 'tg.component/PermissionsSettings/types';
import { confirmation } from 'tg.hooks/confirmation';

type OrganizationModel = components['schemas']['OrganizationModel'];

const messageService = container.resolve(MessageService);

export const OrganizationBasePermissionMenu: FunctionComponent<{
  organization: OrganizationModel;
}> = ({ organization }) => {
  const { t } = useTranslate();

  const { updatePermissions } = useUpdateBasePermissions({
    organizationId: organization.id,
  });

  const confirm = () => {
    return new Promise<void>((resolve, reject) =>
      confirmation({
        message: (
          <T keyName="really_want_to_change_base_permission_confirmation" />
        ),
        hardModeText: organization.name?.toUpperCase(),
        onConfirm: () => {
          resolve();
        },
        onCancel: () => {
          reject();
        },
      })
    );
  };

  async function handleSubmit(data: PermissionSettingsState) {
    await confirm();

    await updatePermissions(data);
    messageService.success(<T keyName="organization_member_privileges_set" />);
  }

  return (
    <PermissionsMenu
      modalProps={{
        onSubmit: handleSubmit,
        title: t('organization_member_privileges_title'),
        permissions: organization.basePermissions,
      }}
    />
  );
};
