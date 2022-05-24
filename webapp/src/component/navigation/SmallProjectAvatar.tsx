import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { components } from 'tg.service/apiSchema.generated';

type ProjectModel = components['schemas']['ProjectModel'];

type Props = {
  project: ProjectModel;
};

export const SmallProjectAvatar: React.FC<Props> = ({ project }) => {
  return (
    <AvatarImg
      owner={{
        name: project.name,
        avatar: project.avatar,
        type: 'PROJECT',
        id: project.id,
      }}
      size={18}
    />
  );
};
