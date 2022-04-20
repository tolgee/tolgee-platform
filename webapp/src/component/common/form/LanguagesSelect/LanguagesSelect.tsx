import { FunctionComponent } from 'react';
import { Select, styled, Typography } from '@mui/material';
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
  value: string[];
  context: string;
};

export const LanguagesSelect: FunctionComponent<Props> = (props) => {
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
    >
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
        })}
      </StyledSelect>
    </FormControl>
  );
};
