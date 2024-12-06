import { Box, Button, DialogActions } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import LoadingButton from 'tg.component/common/form/LoadingButton';

type Props = {
  onDelete?: () => void;
  isDeleting?: boolean;
  onSubmit: () => void;
  isSubmitting: boolean;
  onTest: () => void;
  isTesting: boolean;
  onClose: () => void;
};

export const StorageFormActions = ({
  onDelete,
  isDeleting,
  onSubmit,
  isSubmitting,
  onTest,
  isTesting,
  onClose,
}: Props) => {
  const { t } = useTranslate();
  return (
    <DialogActions sx={{ justifyContent: 'space-between' }}>
      <Box display="flex" gap={1}>
        {onDelete && (
          <LoadingButton
            onClick={onDelete}
            loading={isDeleting}
            variant="outlined"
            data-cy="storage-form-delete"
          >
            {t('storage_form_delete')}
          </LoadingButton>
        )}
        <LoadingButton
          color="default"
          loading={isTesting}
          onClick={onTest}
          variant="outlined"
          data-cy="storage-form-test"
        >
          {t('storage_form_test')}
        </LoadingButton>
      </Box>
      <Box display="flex" gap={1}>
        <Button onClick={onClose}>{t('storage_form_cancel')}</Button>
        <LoadingButton
          variant="contained"
          color="primary"
          onClick={onSubmit}
          loading={isSubmitting}
          data-cy="storage-form-save"
        >
          {t('storage_form_save')}
        </LoadingButton>
      </Box>
    </DialogActions>
  );
};
