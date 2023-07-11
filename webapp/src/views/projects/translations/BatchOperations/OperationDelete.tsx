import { ChevronRight } from '@mui/icons-material';
import { Button } from '@mui/material';

import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import { OperationProps } from './types';

type Props = OperationProps & {
  disabled: boolean;
};

export const OperationDelete = ({ disabled }: Props) => {
  const isDeleting = useTranslationsSelector((c) => c.isDeleting);
  const { deleteTranslations } = useTranslationsActions();

  return (
    <Button
      data-cy="batch-operations-delete-button"
      onClick={deleteTranslations}
      disabled={disabled || isDeleting}
      sx={{ minWidth: 0, minHeight: 0, width: 40, height: 40 }}
      variant="contained"
      color="primary"
    >
      <ChevronRight />
    </Button>
  );
};
