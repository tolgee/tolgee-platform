import { makeStyles } from '@material-ui/core';
import { Close } from '@material-ui/icons';
import { Wrapper } from './Wrapper';

type Props = {
  name: string;
  onDelete?: React.MouseEventHandler<SVGElement>;
};

const useStyles = makeStyles((theme) => ({
  tag: {
    marginLeft: 6,
    marginRight: 6,
    marginTop: -1,
    flexShrink: 1,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  closeIcon: {
    marginLeft: -6,
    padding: 1,
    cursor: 'pointer',
    width: 20,
    height: 20,
    color: theme.palette.text.secondary,
  },
}));

export const Tag: React.FC<Props> = ({ name, onDelete }) => {
  const classes = useStyles();
  return (
    <Wrapper>
      <div className={classes.tag}>{name}</div>
      {onDelete && (
        <Close
          role="button"
          data-cy="translations-tag-close"
          className={classes.closeIcon}
          onClick={onDelete}
        />
      )}
    </Wrapper>
  );
};
