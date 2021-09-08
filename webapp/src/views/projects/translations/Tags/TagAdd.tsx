import { makeStyles } from '@material-ui/core';
import { Add } from '@material-ui/icons';
import { T } from '@tolgee/react';
import clsx from 'clsx';

import { useCellStyles } from '../cell/styles';
import { Wrapper } from './Wrapper';

const useStyles = makeStyles({
  addIcon: {
    fontSize: 16,
    padding: 2,
    width: 20,
    height: 20,
  },
  label: {
    marginTop: -2,
    marginRight: 6,
  },
});

type Props = {
  onClick: () => void;
  withFullLabel: boolean;
  className?: string;
};

export const TagAdd: React.FC<Props> = ({
  onClick,
  withFullLabel,
  className,
}) => {
  const classes = useStyles();
  const cellClasses = useCellStyles({});

  return (
    <Wrapper
      role="add"
      onClick={onClick}
      className={clsx(cellClasses.showOnHover, className)}
    >
      <Add className={classes.addIcon} data-cy="translations-tags-add" />
      {withFullLabel && (
        <div className={classes.label}>
          <T>translations_tag_label</T>
        </div>
      )}
    </Wrapper>
  );
};
