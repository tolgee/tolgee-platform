import { Box, styled } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { AvatarImg } from './common/avatar/AvatarImg';

export type Avatar = components['schemas']['Avatar'];

export type Project = {
  id: number;
  name: string;
  avatar?: Avatar;
};

const StyledOrgItem = styled('div')`
  display: flex;
  gap: 8px;
  align-items: center;
`;

type Props = {
  project: Project;
};

export const ProjectWithAvatar = ({ project }: Props) => {
  return (
    <StyledOrgItem>
      <Box>
        <AvatarImg
          key={0}
          owner={{
            name: project.name,
            avatar: project.avatar,
            type: 'PROJECT',
            id: project.id,
          }}
          size={22}
        />
      </Box>
      <Box>{project.name}</Box>
    </StyledOrgItem>
  );
};
