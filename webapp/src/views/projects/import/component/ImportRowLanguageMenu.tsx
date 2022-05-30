import {
  FormControl,
  FormHelperText,
  IconButton,
  InputAdornment,
  InputLabel,
  MenuItem,
  Select,
  styled,
} from '@mui/material';
import { Add, Clear } from '@mui/icons-material';
import { T } from '@tolgee/react';
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

const StyledItem = styled(MenuItem)`
  padding: ${({ theme }) => theme.spacing(1, 2)};

  &.addNewItem {
    color: ${({ theme }) => theme.palette.primary.main};
  }
`;

const StyledAddIcon = styled(Add)`
  margin-right: ${({ theme }) => theme.spacing(1)};
  margin-left: -2px;
`;

const StyledInputAdornment = styled(InputAdornment)`
  margin-right: ${({ theme }) => theme.spacing(3)};
`;

const NEW_LANGUAGE_VALUE = '__new_language';
export const ImportRowLanguageMenu: React.FC<
  React.PropsWithChildren<{
    value?: number;
    importLanguageId: number;
  }>
> = (props) => {
  const queryClient = useQueryClient();
  const languages = useProjectLanguages();
  const importData = useImportDataHelper();
  const usedLanguages = importData
    .result!._embedded!.languages!.map((l) => l.existingLanguageId)
    .filter((l) => !!l);
  const project = useProject();
  const applyTouched = actions.useSelector((s) => s.applyTouched);
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

  const onChange = (changeEvent: any) => {
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
    <StyledItem value={l.id} key={l.id}>
      {l.flagEmoji} {l.name}
    </StyledItem>
  ));

  items.push(
    <StyledItem key={0} value={NEW_LANGUAGE_VALUE} className="addNewItem">
      <StyledAddIcon fontSize="small" />
      <T>import_result_language_menu_add_new</T>
    </StyledItem>
  );

  return (
    <>
      <FormControl
        fullWidth
        variant="standard"
        error={applyTouched && !props.value}
        data-cy="import-row-language-select-form-control"
      >
        <InputLabel shrink id="import_row_language_select">
          <T>import_language_select</T>
        </InputLabel>
        <Select
          variant="standard"
          endAdornment={
            props.value ? (
              <StyledInputAdornment position="end">
                <IconButton
                  onClick={onReset}
                  size="small"
                  data-cy="import-row-language-select-clear-button"
                >
                  <Clear />
                </IconButton>
              </StyledInputAdornment>
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
