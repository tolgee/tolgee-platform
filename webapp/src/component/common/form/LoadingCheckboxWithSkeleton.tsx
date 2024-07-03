import React, { FC, ReactElement } from 'react';
import {
  Box,
  Checkbox,
  FormControlLabel,
  Skeleton,
  styled,
  Tooltip,
} from '@mui/material';
import { HelpCircle } from '@untitled-ui/icons-react';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';

export type LoadingCheckboxWithSkeletonProps = {
  hint: React.ReactNode;
  label: React.ReactNode;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  checked?: boolean;
  loading: boolean;
  labelProps?: Partial<React.ComponentProps<typeof FormControlLabel>>;
  labelInnerProps?: Partial<React.ComponentProps<typeof StyledLabel>>;
  customHelpIcon?: ReactElement;
} & React.ComponentProps<typeof Checkbox>;

const StyledLabel = styled('div')`
  display: flex;
  gap: 5px;
  align-items: center;
`;

const StyledHelpIcon = styled(HelpCircle)`
  color: ${({ theme }) => theme.palette.tokens.icon.primary};
  width: 16px;
`;

export const LoadingCheckboxWithSkeleton: FC<
  LoadingCheckboxWithSkeletonProps
> = ({
  checked,
  hint,
  label,
  labelInnerProps,
  labelProps,
  loading,
  onChange,
  customHelpIcon,
  ...checkboxProps
}) => {
  return (
    <FormControlLabel
      label={
        <StyledLabel {...labelInnerProps}>
          <div>{label}</div>
          {hint && (
            <Tooltip title={hint}>
              {customHelpIcon || <StyledHelpIcon />}
            </Tooltip>
          )}
        </StyledLabel>
      }
      control={
        <Box position="relative">
          <Box
            sx={(theme) => ({
              position: 'absolute',
              top: 0,
              left: 0,
              bottom: 0,
              right: 0,
              zIndex: 10,
              opacity: checked == undefined || loading ? 1 : 0,
              transition: 'opacity 0.3s',
              paddingLeft: '12px',
              paddingTop: '12px',
              pointerEvents: 'none',
            })}
          >
            {loading ? (
              <SpinnerProgress size={18} />
            ) : (
              <Skeleton
                sx={{
                  borderRadius: '2px',
                  width: '18px',
                  height: '18px',
                }}
                variant="rectangular"
              />
            )}
          </Box>
          <Checkbox
            sx={{
              opacity: checked == undefined || loading ? 0 : 1,
              transition: 'opacity 0.3s',
            }}
            disabled={loading || checked == undefined}
            checked={!!checked}
            onChange={onChange}
            {...checkboxProps}
          />
        </Box>
      }
      {...labelProps}
    />
  );
};
