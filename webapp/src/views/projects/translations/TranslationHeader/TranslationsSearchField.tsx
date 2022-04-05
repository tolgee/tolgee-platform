import { ComponentProps } from 'react';
import { IconButton, InputAdornment, TextField, useTheme } from '@mui/material';
import { Search, Clear } from '@mui/icons-material';
import { T } from '@tolgee/react';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';

const TranslationsSearchField = (
  props: ComponentProps<typeof TextField> & {
    value: string;
    onSearchChange: (value: string) => void;
    style?: React.CSSProperties;
  }
) => {
  const theme = useTheme();

  const { value, onSearchChange, ...otherProps } = props;

  return (
    <TextField
      data-cy="global-search-field"
      label={<T>standard_search_label</T>}
      InputProps={{
        startAdornment: (
          <InputAdornment position="start">
            <Search />
          </InputAdornment>
        ),
        endAdornment: Boolean(value) && (
          <InputAdornment
            position="end"
            style={{ marginRight: -5, marginLeft: 5 }}
          >
            <IconButton
              size="small"
              onClick={stopAndPrevent(() => onSearchChange(''))}
              onMouseDown={stopAndPrevent()}
              edge="start"
            >
              <Clear fontSize="small" />
            </IconButton>
          </InputAdornment>
        ),
        style: {
          paddingLeft: 12,
          fontSize: theme.typography.body2.fontSize,
          height: 40,
          ...props.style,
        },
      }}
      value={value}
      style={{ flexBasis: 200 }}
      onChange={(e) => onSearchChange(e.target.value)}
      {...otherProps}
    />
  );
};

export default TranslationsSearchField;
