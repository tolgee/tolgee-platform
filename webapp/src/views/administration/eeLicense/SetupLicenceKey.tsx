import { Box } from '@mui/material';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { T, useTranslate } from '@tolgee/react';
import { confirmation } from 'tg.hooks/confirmation';

export const SetupLicenceKey = () => {
  const { t } = useTranslate();

  const setKeyMutation = useApiMutation({
    url: '/v2/ee-license/set-license-key',
    method: 'put',
    invalidatePrefix: '/v2/ee-license',
  });

  const prepareKeyMutation = useApiMutation({
    url: '/v2/ee-license/prepare-set-license-key',
    method: 'post',
  });

  function onsSubmit() {
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
                        price: data.plan.pricePerSeat,
                        includedSeats: data.plan.includedSeats,
                      }}
                    />
                  </Box>

                  <Box>
                    <T
                      keyName="ee-license-key-confirmation-message-estimate"
                      params={{
                        additional:
                          data.usage.total - data.usage.subscriptionPrice,
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
    <>
      <StandardForm
        initialValues={{ licenseKey: '' }}
        onSubmit={onsSubmit()}
        submitButtonInner={t('ee_licence_key_apply')}
        hideCancel
      >
        <TextField label={t('ee_licence_key_input_label')} name="licenseKey" />
      </StandardForm>
    </>
  );
};
