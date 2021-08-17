import { makeStyles } from '@material-ui/core';
import { T } from '@tolgee/react';
import { CellStateBar } from './CellStateBar';

const useStyles = makeStyles({
  cell: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    flexGrow: 1,
    justifySelf: 'stretch',
    opacity: 0,
    transition: 'opacity 300ms ease-in-out',
    '&:hover': {
      opacity: 1,
    },
  },
});

type Props = {
  colIndex: number;
  onResize: (colIndex: number) => void;
};

export const EmptyKeyPlaceholder: React.FC<Props> = ({
  onResize,
  colIndex,
}) => {
  const classes = useStyles();
  return (
    <>
      <div className={classes.cell}>
        <T>translations_new_key_empty_message</T>
      </div>
      <CellStateBar onResize={() => onResize(colIndex)} />
    </>
  );
};
