import { Box, styled } from '@mui/material';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { components } from 'tg.service/apiSchema.generated';

type UserAccountModel =
  | components['schemas']['UserAccountModel']
  | components['schemas']['PrivateUserAccountModel'];
type OrganizationModel = components['schemas']['OrganizationModel'];

const StyledContainer = styled('div')`
  display: grid;
  grid-template-areas: 'logo title';
  grid-template-columns: auto 1fr;
  min-height: 42px;
  padding: 10px 16px 6px 16px;
  gap: 0px 8px;
  align-items: center;
`;

const StyledTitle = styled(Box)`
  font-size: 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

type Props = {
  entity: UserAccountModel | OrganizationModel;
  type: 'USER' | 'ORG';
  title?: string;
  subtitle?: string;
};

export const MenuHeader: React.FC<Props> = ({ entity, type, title }) => {
  return (
    <StyledContainer>
      <Box gridArea="logo">
        <AvatarImg
          owner={{
            avatar: entity.avatar,
            id: entity.id,
            name: entity.name,
            type: type,
          }}
          size={26}
        />
      </Box>
      <StyledTitle gridArea="title">{title}</StyledTitle>
    </StyledContainer>
  );
};
