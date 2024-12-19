import { ProfileAvatar } from 'tg.component/common/avatar/ProfileAvatar';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useOrganization } from './useOrganization';

type Props = {
  disabled?: boolean;
};

export const OrganizationProfileAvatar: React.FC<Props> = ({ disabled }) => {
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
    return null;
  }

  return (
    <ProfileAvatar
      owner={{
        type: 'ORG',
        avatar: organization.avatar,
        id: organization.id,
        name: organization.name,
      }}
      disabled={disabled}
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
