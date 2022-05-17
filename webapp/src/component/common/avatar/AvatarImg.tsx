import { AutoAvatar, AutoAvatarType } from './AutoAvatar';
import { AvatarOwner } from './ProfileAvatar';
import { styled } from '@mui/material';

const StyledContainer = styled('div')`
  overflow: hidden;
  display: flex;
`;

export const AvatarImg = (props: {
  size: number;
  owner: AvatarOwner;
  autoAvatarType: AutoAvatarType;
  circle?: boolean;
}) => {
  const background = 'rgb(239, 239, 239)';
  const avatarPath =
    props.size <= 50
      ? props.owner.avatar?.thumbnail
      : props.owner.avatar?.large;

  return (
    <StyledContainer
      style={{
        borderRadius: props.circle ? '50%' : '10%',
        overflow: 'hidden',
        display: 'flex',
      }}
    >
      {avatarPath ? (
        <img
          data-cy={'avatar-image'}
          src={avatarPath}
          alt={props.owner.name || 'Avatar'}
          style={{
            width: props.size,
            height: props.size,
            background,
          }}
        />
      ) : (
        <AutoAvatar
          entityId={`${props.owner.type}-${props.owner.id}`}
          type={props.autoAvatarType}
          size={props.size}
          ownerName={props.owner.name || 'AVATAR'}
          style={{
            background,
          }}
        />
      )}
    </StyledContainer>
  );
};
