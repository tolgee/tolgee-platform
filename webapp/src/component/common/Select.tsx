import React, { ComponentProps } from 'react';
import {
  Box,
  FormHelperText,
  Select as MUISelect,
  selectClasses,
  styled,
  useTheme,
} from '@mui/material';
import {
  StyledContainer,
  StyledInputLabel,
} from 'tg.component/common/TextField';

const StyledMUISelect = styled(MUISelect)`
  .${selectClasses.select} {
    box-sizing: border-box;
  }
`;

type Props = Omit<Partial<ComponentProps<typeof MUISelect>>, 'error'> & {
  minHeight?: boolean;
  error?: string;
  shrinkable?: boolean;
};

export const Select = React.forwardRef(function Select(props: Props, ref) {
  const theme = useTheme();
  const {
    label,
    minHeight = true,
    sx,
    error,
    shrinkable,
    ...otherProps
  } = props;
  const shrinkableColumns = shrinkable ? 'minmax(0, 1fr)' : undefined;

  return (
    <StyledContainer style={{ gridTemplateColumns: shrinkableColumns }}>
      {label && <StyledInputLabel>{label}</StyledInputLabel>}
      <Box
        ref={ref}
        display="grid"
        gridTemplateColumns={shrinkableColumns}
        alignItems="start"
        sx={{ minHeight: minHeight ? '64px' : undefined, ...sx }}
      >
        <StyledMUISelect
          variant="outlined"
          size="small"
          error={Boolean(error)}
          {...otherProps}
        >
          {props.children}
        </StyledMUISelect>
        {error && (
          <FormHelperText sx={{ color: theme.palette.error.main }}>
            {error}
          </FormHelperText>
        )}
      </Box>
    </StyledContainer>
  );
});
