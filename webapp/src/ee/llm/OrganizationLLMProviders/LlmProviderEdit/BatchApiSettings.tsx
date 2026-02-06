import { useTranslate } from '@tolgee/react';
import { Box, Typography, useTheme } from '@mui/material';
import { Switch } from 'tg.component/common/form/fields/Switch';
import { LabelHint } from 'tg.component/common/LabelHint';
import { LlmProviderType, supportsBatchApi } from './llmProvidersConfig';

type Props = {
  providerType: LlmProviderType;
};

export const BatchApiSettings = ({ providerType }: Props) => {
  const { t } = useTranslate();
  const theme = useTheme();

  if (!supportsBatchApi(providerType)) {
    return null;
  }

  return (
    <>
      <Box
        gridColumn="1 / -1"
        height="1px"
        sx={{ backgroundColor: theme.palette.divider }}
        mb={2}
      />
      <Box gridColumn="1 / -1">
        <Typography variant="subtitle2" sx={{ mb: 1 }}>
          {t('llm_provider_batch_api_section')}
        </Typography>
        <Switch
          name="batchApiEnabled"
          label={
            <LabelHint title={t('llm_provider_batch_api_enabled_hint')}>
              {t('llm_provider_batch_api_enabled')}
            </LabelHint>
          }
          data-cy="llm-provider-batch-api-enabled"
        />
      </Box>
    </>
  );
};
