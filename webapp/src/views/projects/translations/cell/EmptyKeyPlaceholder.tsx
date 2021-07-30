import { makeStyles } from '@material-ui/core';
import { Box } from '@material-ui/core';
import { T } from '@tolgee/react';

import { CellContent } from './CellContent';
import { CellPlain } from './CellPlain';

const useStyles = makeStyles({
  cell: {
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
    <Box flexGrow="1" display="flex">
      <CellPlain onResize={() => onResize(colIndex)} state="NONE">
        <CellContent
          alignItems="center"
          justifyContent="center"
          display="flex"
          className={classes.cell}
        >
          <T>translations_new_key_empty_message</T>
        </CellContent>
      </CellPlain>
    </Box>
  );
};
