import { FunctionComponent } from 'react';
import {
  InputLabel,
  MenuProps,
  Select,
  styled,
  Typography,
} from '@mui/material';
import FormControl from '@mui/material/FormControl';

import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { useLanguagesContent } from 'tg.component/common/form/LanguagesSelect/useLanguagesContent';

type LanguageModel = Pick<
  components['schemas']['LanguageModel'],
  'id' | 'tag' | 'name'
> & { base?: boolean };

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
  selectAllToggle?: boolean;
};

export const LanguagesSelect: FunctionComponent<
  React.PropsWithChildren<Props>
> = (props) => {
  const { t } = useTranslate();
  const languagesContent = useLanguagesContent({
    onChange: props.onChange,
    languages: props.languages,
    value: props.value,
    disabledLanguages: props.disabledLanguages,
    enableEmpty: props.enableEmpty,
    context: props.context,
    selectAllToggle: props.selectAllToggle,
  });

  const menuProps: Partial<MenuProps> = {
    variant: 'menu',
    PaperProps: {
      sx: {
        minWidth: 200,
        maxWidth: 250,
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
            {props.selectAllToggle &&
            (selected as string[]).length === props.languages.length
              ? t('languages_permitted_list_select_all')
              : (selected as string[]).join(', ')}
          </StyledTypography>
        )}
        MenuProps={menuProps}
        margin="dense"
      >
        {languagesContent}
      </StyledSelect>
    </FormControl>
  );
};
