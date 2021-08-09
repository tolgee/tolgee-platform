import { makeStyles } from '@material-ui/core';
import { Close } from '@material-ui/icons';

const useStyles = makeStyles({
  closeIcon: {
    fontSize: 16,
    marginLeft: 1,
    cursor: 'pointer',
  },
});

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
