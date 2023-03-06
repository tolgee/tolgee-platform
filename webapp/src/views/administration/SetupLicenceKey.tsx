import { Box, Button } from '@mui/material';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useTranslate } from '@tolgee/react';
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
                    {t('ee-license-key-confirmation-message', {
                      price: data.pricePerSeatMonthly,
                    })}
                  </Box>

                  <Box>
                    Estimated costs by this billing period: // todo translate
                  </Box>
                  <Box>
                    {data.daysUntilPeriodEnd} days until period end * €
                    {data.pricePerSeatDaily} per seat day * {data.seatCount}{' '}
                    seats = €{data.estimatedTotalThisPeriod}
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
        submitButtons={
          <Button variant="contained" color="primary" type="submit">
            {t('ee_licence_key_apply')}
          </Button>
        }
      >
        <TextField label={t('ee_licence_key_input_label')} name="licenseKey" />
      </StandardForm>
    </>
  );
};
