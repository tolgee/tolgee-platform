import React, { FunctionComponent, useEffect } from 'react';
import { useUrlSearch } from 'tg.hooks/useUrlSearch';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useOrganization } from '../useOrganization';

export const OrganizationSlackSuccessHandler: FunctionComponent = () => {
  const search = useUrlSearch();
  const organization = useOrganization();
  const connectMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/slack/connect',
    method: 'post',
  });

  useEffect(() => {
    if (!organization || !search['code'] || Array.isArray(search['code'])) {
      return;
    }
    connectMutation.mutate({
      path: { organizationId: organization.id },
      content: {
        'application/json': { code: search['code'] },
      },
    });
  }, [search['code']]);

  return <></>;
};
