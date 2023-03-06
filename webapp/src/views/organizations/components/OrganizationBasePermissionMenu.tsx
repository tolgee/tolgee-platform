import { FunctionComponent } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { container } from 'tsyringe';

import { PermissionsMenu } from 'tg.component/PermissionsSettings/PermissionsMenu';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useUpdateBasePermissions } from './useUpdateBasePermissions';
import { PermissionSettingsState } from 'tg.component/PermissionsSettings/types';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';

type OrganizationModel = components['schemas']['OrganizationModel'];

const messageService = container.resolve(MessageService);

export const OrganizationBasePermissionMenu: FunctionComponent<{
  organization: OrganizationModel;
}> = ({ organization }) => {
  const messages = useMessage();
  const { t } = useTranslate();

  const { updatePermissions } = useUpdateBasePermissions({
    organizationId: organization.id,
  });

  async function handleSubmit(data: PermissionSettingsState) {
    try {
      await updatePermissions(data);
      messageService.success(<T>organization_member_privileges_set</T>);
    } catch (e) {
      parseErrorResponse(e).forEach((err) => messages.error(<T>{err}</T>));
    }
  }

  return (
    <PermissionsMenu
      onSubmit={handleSubmit}
      permissions={organization.basePermissions}
      title={t('organization_member_privileges_title')}
    />
  );
};
