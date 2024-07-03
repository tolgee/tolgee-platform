import { FunctionComponent, ComponentProps } from 'react';
import {
  Box,
  FormHelperText,
  Select as MUISelect,
  useTheme,
} from '@mui/material';
import {
  StyledContainer,
  StyledInputLabel,
} from 'tg.component/common/TextField';

type Props = Omit<Partial<ComponentProps<typeof MUISelect>>, 'error'> & {
  minHeight?: boolean;
  error?: string;
};

export const Select: FunctionComponent<Props> = (props) => {
  const theme = useTheme();
  const { label, minHeight = true, sx, error, ...otherProps } = props;

  return (
    <StyledContainer>
      {label && <StyledInputLabel>{label}</StyledInputLabel>}
      <Box sx={{ minHeight: minHeight ? '64px' : undefined, ...sx }}>
        <MUISelect
          variant="outlined"
          size="small"
          error={Boolean(error)}
          {...otherProps}
        >
          {props.children}
        </MUISelect>
        {error && (
          <FormHelperText sx={{ color: theme.palette.error.main }}>
            {error}
          </FormHelperText>
        )}
      </Box>
    </StyledContainer>
  );
};
