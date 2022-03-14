import { makeStyles } from '@material-ui/core';
import { Link } from 'react-router-dom';

import { TolgeeLogo } from 'tg.component/common/icons/TolgeeLogo';

const useStyles = makeStyles((theme) => ({
  item: {
    display: 'flex',
    listStyle: 'none',
    flexDirection: 'column',
  },
  link: {
    display: 'flex',
    padding: theme.spacing(1, 0),
    cursor: 'pointer',
    justifyContent: 'center',
    minHeight: theme.mixins.toolbar.minHeight,
    outline: 0,
    transition: 'filter 0.2s ease-in-out',
    '&:focus, &:hover': {
      filter: 'brightness(70%)',
    },
  },
}));

type Props = {
  hidden: boolean;
};

export const SideLogo: React.FC<Props> = ({ hidden }) => {
  const classes = useStyles();
  return (
    <li className={classes.item}>
      <Link to="/" className={classes.link} tabIndex={hidden ? -1 : undefined}>
        <TolgeeLogo fontSize="large" color="primary" />
      </Link>
    </li>
  );
};
