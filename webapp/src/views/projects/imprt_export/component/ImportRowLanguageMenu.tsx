import { ChangeEvent, FunctionComponent } from 'react';
import {
  FormControl,
  FormHelperText,
  IconButton,
  InputAdornment,
  InputLabel,
  makeStyles,
  MenuItem,
  Select,
} from '@material-ui/core';
import { Add, Clear } from '@material-ui/icons';
import { T } from '@tolgee/react';
import clsx from 'clsx';
import { useQueryClient } from 'react-query';
import { container } from 'tsyringe';

import { useStateObject } from 'tg.fixtures/useStateObject';
import { useProject } from 'tg.hooks/useProject';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { invalidateUrlPrefix } from 'tg.service/http/useQueryApi';
import { ImportActions } from 'tg.store/project/ImportActions';

import { useImportDataHelper } from '../hooks/useImportDataHelper';
import { ImportLanguageCreateDialog } from './ImportLanguageCreateDialog';

const actions = container.resolve(ImportActions);

const useStyles = makeStyles((theme) => ({
  item: {
    padding: `${theme.spacing(1)}, ${theme.spacing(2)}`,
  },
  addNewItem: {
    color: theme.palette.primary.main,
  },
  addIcon: {
    marginRight: theme.spacing(1),
    marginLeft: -2,
  },
  selectAdornment: {
    marginRight: theme.spacing(3),
  },
}));
const NEW_LANGUAGE_VALUE = '__new_language';
export const ImportRowLanguageMenu: FunctionComponent<{
  value?: number;
  importLanguageId: number;
}> = (props) => {
  const queryClient = useQueryClient();
  const languages = useProjectLanguages();
  const importData = useImportDataHelper();
  const usedLanguages = importData
    .result!._embedded!.languages!.map((l) => l.existingLanguageId)
    .filter((l) => !!l);
  const project = useProject();
  const applyTouched = actions.useSelector((s) => s.applyTouched);
  const classes = useStyles();
  const state = useStateObject({ addNewLanguageDialogOpen: false });

  const dispatchChange = (value) => {
    actions.loadableActions.selectLanguage.dispatch({
      path: {
        projectId: project.id,
        importLanguageId: props.importLanguageId,
        existingLanguageId: value,
      },
    });
  };

  const onReset = () => {
    actions.loadableActions.resetExistingLanguage.dispatch({
      path: {
        projectId: project.id,
        importLanguageId: props.importLanguageId,
      },
    });
  };

  const onChange = (changeEvent: ChangeEvent<any>) => {
    const value = changeEvent.target.value;
    if (value == NEW_LANGUAGE_VALUE) {
      state.addNewLanguageDialogOpen = true;
      return;
    }
    dispatchChange(value);
  };

  const availableLanguages = languages.filter(
    (lang) => props.value == lang.id || usedLanguages.indexOf(lang.id) < 0
  );

  const items = availableLanguages.map((l) => (
    <MenuItem value={l.id} key={l.id} className={clsx(classes.item)}>
      {l.flagEmoji} {l.name}
    </MenuItem>
  ));

  items.push(
    <MenuItem
      key={0}
      value={NEW_LANGUAGE_VALUE}
      className={clsx(classes.item, classes.addNewItem)}
    >
      <Add fontSize="small" className={classes.addIcon} />
      <T>import_result_language_menu_add_new</T>
    </MenuItem>
  );

  return (
    <>
      <FormControl
        fullWidth
        error={applyTouched && !props.value}
        data-cy="import-row-language-select-form-control"
      >
        <InputLabel shrink id="import_row_language_select">
          <T>import_language_select</T>
        </InputLabel>
        <Select
          endAdornment={
            props.value ? (
              <InputAdornment
                position="end"
                className={classes.selectAdornment}
              >
                <IconButton
                  onClick={onReset}
                  size="small"
                  data-cy="import-row-language-select-clear-button"
                >
                  <Clear />
                </IconButton>
              </InputAdornment>
            ) : (
              <></>
            )
          }
          labelId="import_row_language_select"
          value={props.value || ''}
          onChange={onChange}
          fullWidth
        >
          {items}
        </Select>
        {applyTouched && !props.value && (
          <FormHelperText>
            <T>import_existing_language_not_selected_error</T>
          </FormHelperText>
        )}
      </FormControl>
      <ImportLanguageCreateDialog
        open={state.addNewLanguageDialogOpen}
        onCreated={(id) => {
          // we need to invalidate languages provider
          invalidateUrlPrefix(
            queryClient,
            '/v2/projects/{projectId}/languages'
          );
          dispatchChange(id);
        }}
        onClose={() => (state.addNewLanguageDialogOpen = false)}
      />
    </>
  );
};
