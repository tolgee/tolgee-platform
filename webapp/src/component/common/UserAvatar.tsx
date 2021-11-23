import { makeStyles } from '@material-ui/core';

const NUMBER_OF_LETTERS = 2;
const LIGHTNESS_MAIN = 70;
const LIGHTNESS_BORDER = 90;
const SATURATION = 20;

function getUserInitials(name?: string) {
  const userNames = name?.split(' ') || [];

  return (
    userNames?.length > 1
      ? userNames
          ?.map((name) => name['0'])
          .slice(0, NUMBER_OF_LETTERS)
          .join('')
      : name?.slice(0, NUMBER_OF_LETTERS) || ''
  ).toUpperCase();
}

// STACK OVERFLOW
function stringToHslColor(str, s, l, o = 1) {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }

  const h = hash % 360;
  return `hsla(${h}, ${s}%, ${l}%, ${o})`;
}

const useStyle = makeStyles((theme) => ({
  avatar: {
    display: 'flex',
    width: 24,
    height: 24,
    borderRadius: '50%',
    color: 'black',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: 12,
    flexShrink: 0,
    boxSizing: 'border-box',
    filter: 'drop-shadow(0px 0px 1px rgba(0, 0, 0, 0.2))',
    border: '1px solid white',
    fontWeight: 600,
  },
}));

type Props = {
  fullName: string;
  userName: string;
};

export const UserAvatar: React.FC<Props> = ({ fullName, userName }) => {
  const classes = useStyle();

  const initials = getUserInitials(fullName);
  const background = stringToHslColor(userName, SATURATION, LIGHTNESS_MAIN);
  const borderColor = stringToHslColor(
    userName,
    SATURATION,
    LIGHTNESS_BORDER,
    0.5
  );

  return (
    <div className={classes.avatar} style={{ background, borderColor }}>
      {initials}
    </div>
  );
};
