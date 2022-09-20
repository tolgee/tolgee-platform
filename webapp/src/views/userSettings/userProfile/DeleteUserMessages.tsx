import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { Box, CircularProgress } from '@mui/material';
import { T } from '@tolgee/react';
import React from 'react';

export const DeleteUserMessages = () => {
  const singleOwnerOrganizations = useApiQuery({
    url: '/v2/user/single-owned-organizations',
    method: 'get',
  });

  useGlobalLoading(singleOwnerOrganizations.isFetching);

  if (singleOwnerOrganizations.isLoading) {
    return <CircularProgress />;
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
