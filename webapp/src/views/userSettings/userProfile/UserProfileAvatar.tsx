import React from 'react';
import { ProfileAvatar } from 'tg.component/common/avatar/ProfileAvatar';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useUser } from 'tg.globalContext/helpers';

export const UserProfileAvatar = () => {
  const { refetchInitialData } = useGlobalActions();
  const uploadLoadable = useApiMutation({
    url: '/v2/user/avatar',
    method: 'put',
    options: {
      onSuccess() {
        refetchInitialData();
      },
    },
  });

  const removeLoadable = useApiMutation({
    url: '/v2/user/avatar',
    method: 'delete',
    options: {
      onSuccess() {
        refetchInitialData();
      },
    },
  });

  const user = useUser();

  if (!user) {
    return null;
  }

  return (
    <ProfileAvatar
      owner={{
        type: 'USER',
        avatar: user.avatar,
        id: user.id,
        name: user.name,
      }}
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
