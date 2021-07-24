import React, { ComponentProps, FC, useState } from 'react';
import SearchField from 'tg.component/common/form/fields/SearchField';
import { useTranslate } from '@tolgee/react';
import makeStyles from '@material-ui/core/styles/makeStyles';
import clsx from 'clsx';

const useStyles = makeStyles((theme) => ({
  root: {
    backgroundColor: theme.palette.common.white,
    transition: 'width 0.1s ease-in-out',
    width: 250,
  },
  focused: {
    width: 270,
  },
}));

export const SecondaryBarSearchField: FC<ComponentProps<typeof SearchField>> = (
  props
) => {
  const t = useTranslate();
  const classes = useStyles();
  const [focused, setFocused] = useState(false);

  return (
    <SearchField
      data-cy="global-list-search"
      className={clsx({ [classes.root]: true, [classes.focused]: focused })}
      onFocus={() => {
        setFocused(true);
      }}
      onBlur={() => {
        setFocused(false);
      }}
      placeholder={t('standard_search_label')}
      label={null}
      hiddenLabel={true}
      onSearch={props.onSearch}
      variant={'outlined'}
      size="small"
    />
  );
};
