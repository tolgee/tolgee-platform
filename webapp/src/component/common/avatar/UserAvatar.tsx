import { makeStyles } from '@material-ui/core';
import { useUser } from 'tg.hooks/useUser';
import { AvatarImg } from './AvatarImg';

const SIZE = 24;

const useStyle = makeStyles((theme) => ({
  root: {
    display: 'flex',
    width: SIZE,
    height: SIZE,
    color: 'black',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: 12,
    flexShrink: 0,
    boxSizing: 'border-box',
    fontWeight: 600,
    overflow: 'hidden',
    filter: 'drop-shadow(0px 0px 1px rgba(0, 0, 0, 0.2))',
  },
  img: {
    objectFit: 'cover',
  },
}));

export const UserAvatar: React.FC = () => {
  const classes = useStyle();

  const user = useUser();

  return (
    <div className={classes.root}>
      {user && (
        <AvatarImg
          owner={{
            avatar: user.avatar,
            id: user.id,
            name: user.name,
            type: 'USER',
          }}
          circle={true}
          autoAvatarType="IDENTICON"
          size={SIZE}
        />
      )}
    </div>
  );
};
