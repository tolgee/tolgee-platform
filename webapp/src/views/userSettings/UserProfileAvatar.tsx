import { ProfileAvatar } from 'tg.component/common/avatar/ProfileAvatar';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import React from 'react';
import { useUser } from 'tg.hooks/useUser';

export const UserProfileAvatar = () => {
  const uploadLoadable = useApiMutation({
    url: '/v2/user/avatar',
    method: 'put',
    invalidatePrefix: '/v2/user',
  });

  const removeLoadable = useApiMutation({
    url: '/v2/user/avatar',
    method: 'delete',
    invalidatePrefix: '/v2/user',
  });

  const user = useUser();

  if (!user) {
    return <></>;
  }

  return (
    <ProfileAvatar
      owner={{
        type: 'USER',
        avatar: user.avatar,
        id: user.id,
        name: user.name,
      }}
      circle={true}
      autoAvatarType="IDENTICON"
      onUpload={(blob: Blob) =>
        uploadLoadable.mutateAsync({
          content: {
            'multipart/form-data': {
              avatar: new File([blob], 'Avatar', { type: 'image/png' }) as any,
            },
          },
        })
      }
      onRemove={() => removeLoadable.mutateAsync({})}
    />
  );
};
