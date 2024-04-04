import { Box } from '@mui/material';

import { TextField } from 'tg.component/common/form/fields/TextField';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { T, useTranslate } from '@tolgee/react';
import { confirmation } from 'tg.hooks/confirmation';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { Validation } from 'tg.constants/GlobalValidationSchema';

export const SetupLicenceKey = () => {
  const { t } = useTranslate();

  const { refetchInitialData } = useGlobalActions();

  const setKeyMutation = useApiMutation({
    url: '/v2/ee-license/set-license-key',
    method: 'put',
    invalidatePrefix: '/v2/ee-license',
    options: {
      onSuccess: () => refetchInitialData(),
    },
  });

  const prepareKeyMutation = useApiMutation({
    url: '/v2/ee-license/prepare-set-license-key',
    method: 'post',
  });

  function onSubmit() {
    return (values) => {
      prepareKeyMutation.mutate(
        {
          content: {
            'application/json': { licenseKey: values.licenseKey },
          },
        },
        {
          onSuccess(data) {
            confirmation({
              message: (
                <Box>
                  <Box>
                    <T
                      keyName="ee-license-key-confirmation-message"
                      params={{
                        price: data.plan.prices.perSeat,
                        includedSeats: data.plan.includedUsage.seats,
                      }}
                    />
                  </Box>

                  <Box>
                    <T
                      keyName="ee-license-key-confirmation-message-estimate"
                      params={{
                        additional:
                          data.usage.total - data.usage.subscriptionPrice!,
                      }}
                    />
                  </Box>
                </Box>
              ),
              onConfirm() {
                setKeyMutation.mutate({
                  content: {
                    'application/json': { licenseKey: values.licenseKey },
                  },
                });
              },
            });
          },
        }
      );
    };
  }

  return (
    <StandardForm
      initialValues={{ licenseKey: '' }}
      onSubmit={onSubmit()}
      submitButtonInner={t('ee_licence_key_apply')}
      validationSchema={Validation.EE_LICENSE_FORM}
      hideCancel
    >
      <TextField
        size="small"
        label={t('ee_licence_key_input_label')}
        name="licenseKey"
        inputProps={{ 'data-cy': 'administration-ee-license-key-input' }}
      />
    </StandardForm>
  );
};
