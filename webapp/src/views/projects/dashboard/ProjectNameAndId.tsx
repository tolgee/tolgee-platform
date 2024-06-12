import { styled } from '@mui/material';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { components } from 'tg.service/apiSchema.generated';

type ProjectModel = components['schemas']['ProjectModel'];

const StyledContainer = styled('div')`
  display: flex;
  gap: 12px;
  align-items: baseline;
`;

const StyledAvatarImg = styled(AvatarImg)`
  position: relative;
  top: 3px;
`;

const StyledName = styled('div')`
  font-size: 24px;
`;

const StyledId = styled('div')`
  font-size: 14px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  project: ProjectModel;
};

export const ProjectNameAndId = ({ project }: Props) => {
  return (
    <StyledContainer>
      <StyledAvatarImg
        owner={{
          name: project.name,
          avatar: project.avatar,
          type: 'PROJECT',
          id: project.id,
        }}
        size={24}
      />
      <StyledName>{project.name}</StyledName>
      <StyledId>ID {project.id}</StyledId>
    </StyledContainer>
  );
};
