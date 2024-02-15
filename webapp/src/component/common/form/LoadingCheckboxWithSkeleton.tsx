import React, { FC } from 'react';
import {
  Box,
  Checkbox,
  FormControlLabel,
  Skeleton,
  styled,
  Tooltip,
} from '@mui/material';
import { Help } from '@mui/icons-material';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';

export type LoadingCheckboxWithSkeletonProps = {
  hint: React.ReactNode;
  label: React.ReactNode;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  checked?: boolean;
  loading: boolean;
};

const StyledLabel = styled('div')`
  display: flex;
  gap: 5px;
  align-items: center;
`;

const StyledHelpIcon = styled(Help)`
  font-size: 17px;
`;

export const LoadingCheckboxWithSkeleton: FC<
  LoadingCheckboxWithSkeletonProps
> = (props) => {
  return (
    <FormControlLabel
      label={
        <StyledLabel>
          <div>{props.label}</div>
          {props.hint && (
            <Tooltip title={props.hint}>
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
              opacity: props.checked == undefined || props.loading ? 1 : 0,
              transition: 'opacity 0.3s',
              paddingLeft: '12px',
              paddingTop: '12px',
              pointerEvents: 'none',
            })}
          >
            {props.loading ? (
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
          {/*) : (*/}
          <Checkbox
            sx={{
              opacity: props.checked == undefined || props.loading ? 0 : 1,
              transition: 'opacity 0.3s',
            }}
            disabled={props.loading || props.checked == undefined}
            checked={!!props.checked}
            onChange={props.onChange}
            data-cy="content-delivery-auto-publish-checkbox"
          />
          {/*)}*/}
        </Box>
      }
    />
  );
};
