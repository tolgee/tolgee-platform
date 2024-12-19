import React, { FunctionComponent, useEffect } from 'react';
import { useHistory } from 'react-router-dom';
import { T } from '@tolgee/react';

import { useUrlSearch } from 'tg.hooks/useUrlSearch';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { LINKS, PARAMS } from 'tg.constants/links';
import { messageService } from 'tg.service/MessageService';

import { useOrganization } from '../../useOrganization';

export const OrganizationSlackSuccessHandler: FunctionComponent = () => {
  const search = useUrlSearch();
  const organization = useOrganization();

  const history = useHistory();

  const redirect = () => {
    history.push(
      LINKS.ORGANIZATION_APPS.build({
        [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
      })
    );
  };

  const connectMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/slack/connect',
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/slack',
  });

  useEffect(() => {
    if (!organization || !search['code'] || Array.isArray(search['code'])) {
      return;
    }
    connectMutation.mutate(
      {
        path: { organizationId: organization.id },
        content: {
          'application/json': { code: search['code'] },
        },
      },
      {
        onSuccess() {
          messageService.success(
            <T keyName="slack_organization_connect_success" />
          );
        },
        onSettled() {
          redirect();
        },
      }
    );
  }, [search['code']]);

  return null;
};
