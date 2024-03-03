import React, { FC } from 'react';
import {
  Box,
  Checkbox,
  FormControlLabel,
  Skeleton,
  styled,
  Tooltip,
} from '@mui/material';
import { HelpOutline } from '@mui/icons-material';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';

export type LoadingCheckboxWithSkeletonProps = {
  hint: React.ReactNode;
  label: React.ReactNode;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  checked?: boolean;
  loading: boolean;
  labelProps?: Partial<React.ComponentProps<typeof FormControlLabel>>;
  labelInnerProps?: Partial<React.ComponentProps<typeof StyledLabel>>;
} & React.ComponentProps<typeof Checkbox>;

const StyledLabel = styled('div')`
  display: flex;
  gap: 5px;
  align-items: center;
`;

const StyledHelpIcon = styled(HelpOutline)`
  color: ${({ theme }) => theme.palette.tokens.ICON_PRIMARY};
  font-size: 16px;
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
  ...checkboxProps
}) => {
  return (
    <FormControlLabel
      label={
        <StyledLabel {...labelInnerProps}>
          <div>{label}</div>
          {hint && (
            <Tooltip title={hint}>
              <StyledHelpIcon />
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
