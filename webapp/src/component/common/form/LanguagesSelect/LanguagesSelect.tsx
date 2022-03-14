import { FunctionComponent } from 'react';
import { Select, Typography } from '@material-ui/core';
import FormControl from '@material-ui/core/FormControl';
import makeStyles from '@material-ui/core/styles/makeStyles';

import { components } from 'tg.service/apiSchema.generated';
import { getLanguagesContent } from './getLanguagesContent';

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

export type Props = {
  onChange: (value: string[]) => void;
  languages: LanguageModel[];
  value: string[];
  context: string;
};

export const LanguagesSelect: FunctionComponent<Props> = (props) => {
  const classes = useStyles();

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
        {getLanguagesContent({
          onChange: props.onChange,
          languages: props.languages,
          value: props.value,
        })}
      </Select>
    </FormControl>
  );
};
