import React, { FunctionComponent, useEffect } from 'react';
import { useHistory } from 'react-router-dom';
import { useUrlSearch } from 'tg.hooks/useUrlSearch';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useOrganization } from '../useOrganization';
import { LINKS, PARAMS } from 'tg.constants/links';

export const OrganizationSlackSuccessHandler: FunctionComponent = () => {
  const search = useUrlSearch();
  const organization = useOrganization();

  const history = useHistory();

  const redirect = () => {
    history.push(
      LINKS.ORGANIZATION_SLACK.build({
        [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
      })
    );
  };

  const connectMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/slack/connect',
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/slack',
    options: {
      onSettled: redirect,
    },
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
