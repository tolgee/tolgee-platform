import makeStyles from '@mui/styles/makeStyles';
import { Close } from '@mui/icons-material';
import { Theme } from '@mui/material';

const useStyles = makeStyles<Theme>((theme) => ({
  closeIcon: {
    fontSize: 20,
    cursor: 'pointer',
    padding: 2,
    color: theme.palette.text.secondary,
  },
}));

type Props = {
  onClick?: React.MouseEventHandler<SVGElement>;
};

export const CloseButton: React.FC<Props> = ({ onClick }) => {
  const classes = useStyles();

  return (
    <Close
      role="button"
      data-cy="translations-tag-close"
      className={classes.closeIcon}
      onClick={onClick}
    />
  );
};
