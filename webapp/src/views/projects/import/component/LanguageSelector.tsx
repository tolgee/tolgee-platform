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
import { Plus, XClose } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';
import { useQueryClient } from 'react-query';

import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { invalidateUrlPrefix } from 'tg.service/http/useQueryApi';

import { useImportDataHelper } from '../hooks/useImportDataHelper';
import { useImportLanguageHelper } from '../hooks/useImportLanguageHelper';
import { components } from 'tg.service/apiSchema.generated';
import { LanguagesAddDialog } from 'tg.component/languages/LanguagesAddDialog';
import { useState } from 'react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';

const StyledItem = styled(MenuItem)`
  padding: ${({ theme }) => theme.spacing(1, 2)};

  &.addNewItem {
    color: ${({ theme }) => theme.palette.primary.main};
  }
`;

const StyledItemContent = styled('div')`
  gap: 10px;
  display: flex;
  align-items: center;
`;

const StyledPlusIcon = styled(Plus)`
  margin-right: ${({ theme }) => theme.spacing(1)};
  margin-left: -2px;
`;

const StyledInputAdornment = styled(InputAdornment)`
  margin-right: ${({ theme }) => theme.spacing(3)};
`;

const NEW_LANGUAGE_VALUE = '__new_language';
export const LanguageSelector: React.FC<{
  value?: number;
  row: components['schemas']['ImportLanguageModel'];
}> = (props) => {
  const queryClient = useQueryClient();
  const languages = useProjectLanguages();
  const importData = useImportDataHelper();
  const languageHelper = useImportLanguageHelper(props.row);
  const existingLanguages = useProjectLanguages();

  const [dialogOpen, setDialogOpen] = useState(false);

  const onChange = (changeEvent: any) => {
    const value = changeEvent.target.value;
    if (value == NEW_LANGUAGE_VALUE) {
      setDialogOpen(true);
    } else {
      languageHelper.onSelectExisting(value);
    }
  };

  const items = languages.map((l) => (
    <StyledItem value={l.id} key={l.id}>
      <StyledItemContent>
        <FlagImage height={16} flagEmoji={l.flagEmoji || ''} />
        <span>{l.name}</span>
      </StyledItemContent>
    </StyledItem>
  ));

  items.push(
    <StyledItem key={0} value={NEW_LANGUAGE_VALUE} className="addNewItem">
      <StyledPlusIcon width={18} height={18} />
      <T keyName="import_result_language_menu_add_new" />
    </StyledItem>
  );

  return (
    <>
      <FormControl
        fullWidth
        variant="standard"
        error={importData.applyTouched && !props.value}
        data-cy="import-row-language-select-form-control"
      >
        <InputLabel shrink id="import_row_language_select">
          <T keyName="import_language_select" />
        </InputLabel>
        <Select
          variant="standard"
          endAdornment={
            props.value ? (
              <StyledInputAdornment position="end">
                <IconButton
                  onClick={languageHelper.onResetExisting}
                  size="small"
                  data-cy="import-row-language-select-clear-button"
                >
                  <XClose />
                </IconButton>
              </StyledInputAdornment>
            ) : null
          }
          labelId="import_row_language_select"
          value={props.value || ''}
          onChange={onChange}
          fullWidth
        >
          {items}
        </Select>
        {importData.applyTouched && !props.value && (
          <FormHelperText>
            <T keyName="import_existing_language_not_selected_error" />
          </FormHelperText>
        )}
      </FormControl>
      {dialogOpen && (
        <LanguagesAddDialog
          onClose={() => setDialogOpen(false)}
          onChangesMade={() => {
            invalidateUrlPrefix(
              queryClient,
              '/v2/projects/{projectId}/languages'
            );
          }}
          onCreated={(langs) => {
            if (langs.length === 1) {
              // if user adds exactly one language, put it into select
              languageHelper.onSelectExisting(langs[0].id);
            }
          }}
          existingLanguages={existingLanguages.map((l) => l.tag)}
        />
      )}
    </>
  );
};
