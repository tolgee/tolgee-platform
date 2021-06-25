import {
  Box,
  Checkbox,
  ListItemText,
  MenuItem,
  Select,
  Theme,
} from '@material-ui/core';
import FormControl from '@material-ui/core/FormControl';
import Input from '@material-ui/core/Input';
import InputLabel from '@material-ui/core/InputLabel';
import makeStyles from '@material-ui/core/styles/makeStyles';
import { T } from '@tolgee/react';
import { FunctionComponent, useState } from 'react';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { MessageService } from 'tg.service/MessageService';
import { TranslationActions } from 'tg.store/project/TranslationActions';
import { container } from 'tsyringe';

export interface LanguagesMenuProps {
  context: string;
}

const actions = container.resolve(TranslationActions);

const useStyles = makeStyles((theme: Theme) => ({
  input: {
    minWidth: 200,
  },
}));

const messaging = container.resolve(MessageService);

export const LanguagesMenu: FunctionComponent<LanguagesMenuProps> = (props) => {
  const classes = useStyles({});

  const languageDTOS = useProjectLanguages();

  const selected = actions.useSelector((s) => s.selectedLanguages);

  const [localSelected, setLocalSelected] = useState(selected || []);

  const langsChange = (e) => {
    if (e.target.value < 1) {
      messaging.error(<T>set_at_least_one_language_error</T>);
      return;
    }
    setLocalSelected(e.target.value);
  };

  const onLanguageMenuExit = () => {
    actions.select.dispatch(localSelected);
  };

  const menuProps = {
    PaperProps: {
      style: {
        maxHeight: 300,
        width: 250,
      },
    },
    id: `language-select-${props.context}-menu`,
    onExit: onLanguageMenuExit,
  };

  return (
    <Box display="flex" alignItems="right">
      <FormControl data-cy="translations-language-select-form-control">
        <InputLabel id="languages">
          <T>translations_language_select_label</T>
        </InputLabel>
        <Select
          labelId={`languages-${props.context}`}
          id={`languages-select-${props.context}`}
          multiple
          value={localSelected}
          onChange={(e) => langsChange(e)}
          input={<Input className={classes.input} />}
          renderValue={(selected) =>
            (selected as string[])
              .map(
                (val) =>
                  // @ts-ignore
                  languageDTOS.find((languageDTO) => languageDTO.tag === val)
                    .name
              )
              .join(', ')
          }
          MenuProps={menuProps}
        >
          {languageDTOS.map((lang) => (
            <MenuItem key={lang.tag} value={lang.tag}>
              <Checkbox checked={localSelected.indexOf(lang.tag) > -1} />
              <ListItemText primary={lang.name} />
            </MenuItem>
          ))}
        </Select>
      </FormControl>
    </Box>
  );
};
