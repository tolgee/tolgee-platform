import LoadingButton from 'tg.component/common/form/LoadingButton';
import { Refresh } from '@mui/icons-material';
import { Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useSuccessMessage } from 'tg.hooks/useSuccessMessage';

export const RefreshButton = () => {
  const { t } = useTranslate();

  const successMessage = useSuccessMessage();

  const refreshMutation = useApiMutation({
    url: '/v2/ee-license/refresh',
    method: 'put',
    invalidatePrefix: '/v2/ee-license',
  });

  function onClick() {
    refreshMutation.mutate(
      {},
      {
        onSuccess: () =>
          successMessage(t('ee-license-refresh-success-message')),
      }
    );
  }

  return (
    <LoadingButton
      onClick={onClick}
      loading={refreshMutation.isLoading}
      variant="outlined"
    >
      <Tooltip title={t('ee-license-refresh-tooltip')}>
        <Refresh />
      </Tooltip>
    </LoadingButton>
  );
};
