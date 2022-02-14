import { FunctionComponent } from 'react';
import {
  Checkbox,
  ListItemText,
  MenuItem,
  Select,
  Typography,
} from '@material-ui/core';
import { T } from '@tolgee/react';
import { container } from 'tsyringe';
import FormControl from '@material-ui/core/FormControl';
import makeStyles from '@material-ui/core/styles/makeStyles';

import { components } from 'tg.service/apiSchema.generated';
import { MessageService } from 'tg.service/MessageService';
import { putBaseLangFirst } from 'tg.fixtures/putBaseLangFirst';

type LanguageModel = components['schemas']['LanguageModel'];

const useStyles = makeStyles({
  input: {
    display: 'flex',
    minWidth: 80,
    width: 200,
    height: 40,
    flexShrink: 1,
    maxWidth: '100%',
    '& .MuiSelect-root': {
      display: 'flex',
      alignItems: 'center',
      overflow: 'hidden',
      position: 'relative',
    },
  },
  inputContent: {
    overflow: 'hidden',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
    margin: '-4px 0px',
  },
});

export interface LanguagesMenuProps {
  onChange: (value: string[]) => void;
  languages: LanguageModel[];
  value: string[];
  context: string;
}

const messaging = container.resolve(MessageService);

export const LanguagesMenu: FunctionComponent<LanguagesMenuProps> = (props) => {
  const classes = useStyles();
  const baseLang = props.languages.find((l) => l.base)?.tag;

  const langsChange = (e) => {
    if (e.target.value < 1) {
      messaging.error(<T>set_at_least_one_language_error</T>);
      return;
    }
    const langs = putBaseLangFirst(e.target.value, baseLang) || [];
    props.onChange(langs);
  };

  const menuProps = {
    variant: 'menu',
    getContentAnchorEl: null,
    PaperProps: {
      style: {
        width: 250,
      },
    },
    anchorOrigin: {
      vertical: 'bottom',
      horizontal: 'left',
    },
    id: `language-select-${props.context}-menu`,
  } as const;

  return (
    <FormControl
      data-cy="translations-language-select-form-control"
      variant="outlined"
      size="small"
    >
      <Select
        className={classes.input}
        labelId={`languages-${props.context}`}
        id={`languages-select-${props.context}`}
        multiple
        value={props.value}
        onChange={(e) => langsChange(e)}
        renderValue={(selected) => (
          <Typography
            color="textPrimary"
            variant="body1"
            className={classes.inputContent}
          >
            {(selected as string[]).join(', ')}
          </Typography>
        )}
        MenuProps={menuProps}
        margin="dense"
      >
        {props.languages.map((lang) => (
          <MenuItem
            key={lang.tag}
            value={lang.tag}
            data-cy="translations-language-select-item"
          >
            <Checkbox
              checked={props.value.indexOf(lang.tag) > -1}
              size="small"
            />
            <ListItemText primary={lang.name} />
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
};
