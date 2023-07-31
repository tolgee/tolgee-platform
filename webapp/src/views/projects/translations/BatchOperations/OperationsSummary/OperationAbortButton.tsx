import clsx from 'clsx';
import { Box, CircularProgress, styled } from '@mui/material';
import { Close } from '@mui/icons-material';

import { components } from 'tg.service/apiSchema.generated';
import { useOperationCancel } from './useOperationCancel';

type BatchJobModel = components['schemas']['BatchJobModel'];

const AbortButton = styled(Box)`
  cursor: pointer;
  display: flex;
  align-items: center;
  margin: 0px -5px;
  width: 20px;
  height: 20px;

  &.disabled {
    pointer-events: none;
    color: ${({ theme }) => theme.palette.emphasis[800]};
  }
`;

type Props = {
  operation: BatchJobModel;
};

export function OperationAbortButton({ operation }: Props) {
  const { handleCancel, loading, cancelable } = useOperationCancel({
    operation,
  });

  if (!cancelable) {
    return null;
  }

  return (
    <AbortButton
      role="button"
      onClick={handleCancel}
      className={clsx({ disabled: loading })}
    >
      {!loading ? (
        <Close fontSize="small" color="inherit" />
      ) : (
        <CircularProgress size={18} />
      )}
    </AbortButton>
  );
}
