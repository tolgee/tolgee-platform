import LoadingButton from 'tg.component/common/form/LoadingButton';
import { T } from '@tolgee/react';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { confirmation } from 'tg.hooks/confirmation';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';

export const ReleaseKeyButton = () => {
  const { refetchInitialData } = useGlobalActions();

  const releaseKey = useApiMutation({
    url: '/v2/ee-license/release-license-key',
    method: 'put',
    invalidatePrefix: '/v2/ee-license',
    options: { onSuccess: () => refetchInitialData() },
  });

  function onClick() {
    confirmation({
      message: <T keyName="ee-license-release-key-confirmation" />,
      onConfirm() {
        releaseKey.mutate({});
      },
    });
  }

  return (
    <LoadingButton
      onClick={onClick}
      loading={releaseKey.isLoading}
      size="small"
      variant="outlined"
      color="error"
      data-cy="administration-ee-license-release-key-button"
    >
      <T keyName="ee-license-release-key-button" />
    </LoadingButton>
  );
};
