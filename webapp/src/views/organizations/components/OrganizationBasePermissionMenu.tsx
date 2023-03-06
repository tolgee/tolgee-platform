import { FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import { useRouteMatch } from 'react-router-dom';
import { container } from 'tsyringe';

import { PermissionsMenu } from 'tg.component/PermissionsSettings/PermissionsMenu';
import { PARAMS } from 'tg.constants/links';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useUpdateBasePermissions } from './useUpdateBasePermissions';
import { PermissionSettingsState } from 'tg.component/PermissionsSettings/types';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';

type OrganizationModel = components['schemas']['OrganizationModel'];

const messageService = container.resolve(MessageService);

export const OrganizationBasePermissionMenu: FunctionComponent<{
  organization: OrganizationModel;
}> = (props) => {
  const match = useRouteMatch();
  const organizationSlug = match.params[PARAMS.ORGANIZATION_SLUG];
  const messages = useMessage();

  const organization = useApiQuery({
    url: '/v2/organizations/{slug}',
    method: 'get',
    path: { slug: organizationSlug },
  });

  const { updatePermissions } = useUpdateBasePermissions({
    organizationId: organization.data?.id,
  });

  function handleSubmit(data: PermissionSettingsState) {
    return updatePermissions(data)
      .then(() => {
        messageService.success(<T>organization_member_privileges_set</T>);
      })
      .catch((e) => {
        parseErrorResponse(e).forEach((err) => messages.error(<T>{err}</T>));
      });
  }

  if (!organization.data) {
    return <FullPageLoading />;
  }

  return (
    <PermissionsMenu
      onSubmit={handleSubmit}
      permissions={organization.data!.basePermissions}
    />
  );
};
