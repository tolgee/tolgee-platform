import clsx from 'clsx';
import { Box, styled } from '@mui/material';
import { XClose } from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';
import { useOperationCancel } from './useOperationCancel';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';

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
        <XClose width={18} height={18} />
      ) : (
        <SpinnerProgress size={18} />
      )}
    </AbortButton>
  );
}
