import { ComponentProps, FC } from 'react';
import SearchField from 'tg.component/common/form/fields/SearchField';
import { useTranslate } from '@tolgee/react';
import makeStyles from '@mui/styles/makeStyles';
import { Theme } from '@mui/material';

const useStyles = makeStyles<Theme>((theme) => ({
  root: {
    backgroundColor: theme.palette.common.white,
    transition: 'width 0.1s ease-in-out',
    width: 250,
  },
}));

export const SecondaryBarSearchField: FC<ComponentProps<typeof SearchField>> = (
  props
) => {
  const t = useTranslate();
  const classes = useStyles();

  return (
    <SearchField
      data-cy="global-list-search"
      className={classes.root}
      placeholder={t('standard_search_label')}
      label={null}
      hiddenLabel={true}
      onSearch={props.onSearch}
      variant={'outlined'}
      size="small"
    />
  );
};
