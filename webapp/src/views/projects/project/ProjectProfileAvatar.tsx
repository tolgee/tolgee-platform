import { ProfileAvatar } from 'tg.component/common/avatar/ProfileAvatar';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import React from 'react';
import { useProject } from 'tg.hooks/useProject';

export const ProjectProfileAvatar = () => {
  const uploadLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/avatar',
    method: 'put',
    invalidatePrefix: '/v2/projects',
  });

  const removeLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/avatar',
    method: 'delete',
    invalidatePrefix: '/v2/projects',
  });

  const project = useProject();

  if (!project) {
    return null;
  }

  return (
    <ProfileAvatar
      owner={{
        type: 'PROJECT',
        avatar: project.avatar,
        id: project.id,
        name: project.name,
      }}
      onUpload={(blob: Blob) =>
        uploadLoadable.mutateAsync({
          path: {
            projectId: project.id,
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
            projectId: project.id,
          },
        })
      }
    />
  );
};
