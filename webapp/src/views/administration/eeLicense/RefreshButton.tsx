import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useTranslate, T } from '@tolgee/react';
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
          successMessage(<T keyName="ee-license-refresh-success-message" />),
      }
    );
  }

  return (
    <LoadingButton
      onClick={onClick}
      loading={refreshMutation.isLoading}
      variant="outlined"
      size="small"
    >
      {t('ee-license-refresh-tooltip')}
    </LoadingButton>
  );
};
