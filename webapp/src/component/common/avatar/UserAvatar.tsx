import { styled } from '@mui/material';
import { useUser } from 'tg.globalContext/helpers';
import { AvatarImg } from './AvatarImg';

const SIZE = 24;

const StyledContainer = styled('div')<{ size: number }>`
  display: flex;
  width: ${(props) => props.size}px;
  height: ${(props) => props.size}px;
  color: black;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
  box-sizing: border-box;
  font-weight: 600;
  overflow: hidden;
  filter: drop-shadow(0px 0px 1px rgba(0, 0, 0, 0.2));
`;

type Props = {
  size?: number;
};

export const UserAvatar: React.FC<Props> = ({ size = SIZE }) => {
  const user = useUser();

  return (
    <StyledContainer size={size}>
      {user && (
        <AvatarImg
          owner={{
            avatar: user.avatar,
            id: user.id,
            name: user.name,
            type: 'USER',
          }}
          size={size}
        />
      )}
    </StyledContainer>
  );
};
