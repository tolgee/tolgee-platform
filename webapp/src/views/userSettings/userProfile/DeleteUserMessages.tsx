import { T } from '@tolgee/react';
import { Box } from '@mui/material';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';

export const DeleteUserMessages = () => {
  const singleOwnerOrganizations = useApiQuery({
    url: '/v2/user/single-owned-organizations',
    method: 'get',
  });

  if (singleOwnerOrganizations.isLoading) {
    return <SpinnerProgress />;
  }

  const organizations =
    singleOwnerOrganizations?.data?._embedded?.organizations;

  return (
    <Box>
      <Box>
        <T keyName="delete-user-confirmation-text" />
      </Box>
      {organizations?.length && (
        <>
          <T keyName="organizations-will-be-deleted" />
          <ul>
            {organizations?.map((organization) => (
              <li
                data-cy={'user-delete-organization-message-item'}
                key={organization.id}
              >
                {organization.name}
              </li>
            ))}
          </ul>
        </>
      )}
    </Box>
  );
};
