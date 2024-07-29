import { useTranslate, T } from '@tolgee/react';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useSuccessMessage } from 'tg.hooks/useSuccessMessage';
import { Box, IconButton, Tooltip } from '@mui/material';
import { Refresh } from '@mui/icons-material';

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
          successMessage(<T keyName="ee-license-refresh-success-message" />),
      }
    );
  }

  return (
    <Box display="grid" justifyContent="flex-end" alignItems="flex-start">
      <Tooltip title={t('ee-license-refresh-tooltip')}>
        <IconButton
          onClick={onClick}
          disabled={refreshMutation.isLoading}
          size="small"
        >
          <Refresh />
        </IconButton>
      </Tooltip>
    </Box>
  );
};
