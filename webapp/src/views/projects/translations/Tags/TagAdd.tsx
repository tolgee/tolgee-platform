import { makeStyles } from '@material-ui/core';
import { Add } from '@material-ui/icons';
import { T } from '@tolgee/react';

import { useCellStyles } from '../cell/styles';
import { Wrapper } from './Wrapper';

const useStyles = makeStyles((theme) => ({
  addIcon: {
    fontSize: 16,
    margin: -1,
  },
}));

type Props = {
  onClick: () => void;
  withFullLabel: boolean;
};

export const TagAdd: React.FC<Props> = ({ onClick, withFullLabel }) => {
  const classes = useStyles();
  const cellClasses = useCellStyles();

  return (
    <Wrapper role="add" onClick={onClick} className={cellClasses.showOnHover}>
      <Add className={classes.addIcon} data-cy="translations-tags-add" />
      {withFullLabel && <T>translations_tag_label</T>}
    </Wrapper>
  );
};
