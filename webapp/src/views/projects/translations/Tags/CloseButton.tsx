import { makeStyles } from '@material-ui/core';
import { Close } from '@material-ui/icons';

const useStyles = makeStyles((theme) => ({
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
