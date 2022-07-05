import React from 'react';
import { ProfileAvatar } from 'tg.component/common/avatar/ProfileAvatar';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useInitialDataDispatch, useUser } from 'tg.hooks/InitialDataProvider';

export const UserProfileAvatar = () => {
  const initialDataDispatch = useInitialDataDispatch();
  const uploadLoadable = useApiMutation({
    url: '/v2/user/avatar',
    method: 'put',
    options: {
      onSuccess() {
        initialDataDispatch({ type: 'REFETCH' });
      },
    },
  });

  const removeLoadable = useApiMutation({
    url: '/v2/user/avatar',
    method: 'delete',
    options: {
      onSuccess() {
        initialDataDispatch({ type: 'REFETCH' });
      },
    },
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
