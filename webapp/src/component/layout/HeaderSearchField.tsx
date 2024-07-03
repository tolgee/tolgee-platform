import { ComponentProps } from 'react';
import { IconButton, InputAdornment, useTheme } from '@mui/material';
import { SearchSm, XClose } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { TextField } from 'tg.component/common/TextField';

export const HeaderSearchField = (
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
      fullWidth
      data-cy="global-search-field"
      label={<T keyName="standard_search_label" />}
      minHeight={false}
      InputProps={{
        startAdornment: (
          <InputAdornment position="start">
            <SearchSm width={20} height={20} />
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
              <XClose width={20} height={20} />
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
