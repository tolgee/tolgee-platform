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

import { MessageService } from 'tg.service/MessageService';

const useStyles = makeStyles({
  input: {
    display: 'flex',
    minWidth: 80,
    width: 200,
    height: 40,
    flexShrink: 1,
    maxWidth: '100%',
  },
  inputContent: {
    overflow: 'hidden',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
    margin: '-4px 0px',
  },
});

type Language = {
  value: string;
  label: string;
};
export interface LanguagesMenuProps {
  onChange: (value: string[]) => void;
  languages: Language[];
  value: string[];
  context: string;
}

const messaging = container.resolve(MessageService);

export const LanguagesMenu: FunctionComponent<LanguagesMenuProps> = (props) => {
  const classes = useStyles();

  const langsChange = (e) => {
    if (e.target.value < 1) {
      messaging.error(<T>set_at_least_one_language_error</T>);
      return;
    }
    props.onChange(e.target.value);
  };

  const menuProps = {
    variant: 'menu',
    PaperProps: {
      style: {
        width: 250,
      },
    },
    id: `language-select-${props.context}-menu`,
  } as const;

  return (
    <FormControl
      data-cy="translations-language-select-form-control"
      variant="outlined"
      size="small"
      className={classes.input}
    >
      <Select
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
            key={lang.value}
            value={lang.value}
            data-cy="translations-language-select-item"
          >
            <Checkbox
              checked={props.value.indexOf(lang.value) > -1}
              size="small"
            />
            <ListItemText primary={lang.label} />
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
};
