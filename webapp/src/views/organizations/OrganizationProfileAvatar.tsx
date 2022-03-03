import { ProfileAvatar } from 'tg.component/common/avatar/ProfileAvatar';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import React from 'react';
import { useOrganization } from './useOrganization';

export const OrganizationProfileAvatar = () => {
  const uploadLoadable = useApiMutation({
    url: '/v2/organizations/{id}/avatar',
    method: 'put',
    invalidatePrefix: '/v2/organizations',
  });

  const removeLoadable = useApiMutation({
    url: '/v2/organizations/{id}/avatar',
    method: 'delete',
    invalidatePrefix: '/v2/organizations',
  });

  const organization = useOrganization();

  if (!organization) {
    return <></>;
  }

  return (
    <ProfileAvatar
      owner={{
        type: 'ORG',
        avatar: organization.avatar,
        id: organization.id,
        name: organization.name,
      }}
      autoAvatarType="INITIALS"
      onUpload={(blob: Blob) =>
        uploadLoadable.mutateAsync({
          path: {
            id: organization.id,
          },
          content: {
            'multipart/form-data': {
              avatar: new File([blob], 'Avatar', { type: 'image/png' }) as any,
            },
          },
        })
      }
      onRemove={() =>
        removeLoadable.mutateAsync({
          path: {
            id: organization.id,
          },
        })
      }
    />
  );
};
