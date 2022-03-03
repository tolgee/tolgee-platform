import { ProfileAvatar } from 'tg.component/common/avatar/ProfileAvatar';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import React from 'react';
import { useProject } from 'tg.hooks/useProject';

export const ProjectProfileAvatar = () => {
  const uploadLoadable = useApiMutation({
    url: '/v2/projects/{id}/avatar',
    method: 'put',
    invalidatePrefix: '/v2/projects',
  });

  const removeLoadable = useApiMutation({
    url: '/v2/projects/{id}/avatar',
    method: 'delete',
    invalidatePrefix: '/v2/projects',
  });

  const project = useProject();

  if (!project) {
    return <></>;
  }

  return (
    <ProfileAvatar
      owner={{
        type: 'PROJECT',
        avatar: project.avatar,
        id: project.id,
        name: project.name,
      }}
      autoAvatarType="INITIALS"
      onUpload={(blob: Blob) =>
        uploadLoadable.mutateAsync({
          path: {
            id: project.id,
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
            id: project.id,
          },
        })
      }
    />
  );
};
