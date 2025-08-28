import { FunctionComponent } from 'react';
import {
  InputLabel,
  MenuProps,
  Select,
  styled,
  Typography,
} from '@mui/material';
import FormControl from '@mui/material/FormControl';

import { components } from 'tg.service/apiSchema.generated';
import { getLanguagesContent } from './getLanguagesContent';

type LanguageModel = components['schemas']['LanguageModel'];

const StyledSelect = styled(Select)`
  display: flex;
  min-width: 80px;
  width: 200px;
  height: 40px;
  flex-shrink: 1;
  max-width: 100%;
  & .MuiSelect-root {
    display: flex;
    align-items: center;
    overflow: hidden;
    position: relative;
  }
`;

const StyledTypography = styled(Typography)`
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  margin: -2px 0px;
`;

export type Props = {
  onChange: (value: string[]) => void;
  languages: LanguageModel[];
  disabledLanguages?: number[] | undefined;
  value: string[];
  context: string;
  enableEmpty?: boolean;
  placeholder?: string;
  placement?: 'top';
  className?: string;
};

export const LanguagesSelect: FunctionComponent<Props> = (props) => {
  const menuProps: Partial<MenuProps> = {
    variant: 'menu',
    PaperProps: {
      style: {
        width: 250,
      },
    },
    id: `language-select-${props.context}-menu`,
    anchorOrigin: {
      vertical: props.placement === 'top' ? 'top' : 'bottom',
      horizontal: 'center',
    },
    transformOrigin: {
      vertical: props.placement === 'top' ? 'bottom' : 'top',
      horizontal: 'center',
    },
  };

  return (
    <FormControl
      data-cy="translations-language-select-form-control"
      variant="outlined"
      size="small"
      className={props.className}
    >
      {props.placeholder && props.value.length === 0 && (
        <InputLabel focused={false} shrink={false}>
          {props.placeholder}
        </InputLabel>
      )}
      <StyledSelect
        labelId={`languages-${props.context}`}
        id={`languages-select-${props.context}`}
        multiple
        value={props.value}
        renderValue={(selected) => (
          <StyledTypography color="textPrimary" variant="body1">
            {(selected as string[]).join(', ')}
          </StyledTypography>
        )}
        MenuProps={menuProps}
        margin="dense"
      >
        {getLanguagesContent({
          onChange: props.onChange,
          languages: props.languages,
          value: props.value,
          disabledLanguages: props.disabledLanguages,
          enableEmpty: props.enableEmpty,
          context: props.context,
        })}
      </StyledSelect>
    </FormControl>
  );
};
