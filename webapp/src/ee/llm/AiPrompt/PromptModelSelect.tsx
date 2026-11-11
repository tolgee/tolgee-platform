import { Box, MenuItem, Select, Tooltip, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { InfoCircle } from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';

type ProvidersModel =
  components['schemas']['CollectionModelLlmProviderSimpleModel'];

type Props = {
  provider: string;
  onProviderChange: (provider: string) => void;
  providers: ProvidersModel | undefined;
};

export const PromptModelSelect = ({
  provider,
  onProviderChange,
  providers,
}: Props) => {
  const { t } = useTranslate();

  return (
    <Box display="grid" gap={0.5}>
      <Box display="flex" alignItems="center" gap={0.5}>
        <Typography variant="caption" color="textSecondary">
          {t('ai_prompt_model_label', 'Model')}
        </Typography>
        <Tooltip
          title={t('ai_prompt_model_tooltip', 'Each prompt has its own model.')}
          disableInteractive
        >
          <Box display="flex">
            <InfoCircle width={16} height={16} />
          </Box>
        </Tooltip>
      </Box>
      <Select
        size="small"
        value={provider}
        onChange={(e) => onProviderChange(e.target.value)}
        sx={{ width: '50%' }}
        data-cy="ai-prompt-provider-select"
      >
        {providers?._embedded?.providers?.map((i) => (
          <MenuItem
            key={i.name}
            value={i.name}
            data-cy="ai-prompt-provider-item"
          >
            {i.resolvesToName
              ? t('ai_prompt_provider_server_default', 'default ({name})', {
                  name: i.resolvesToName,
                })
              : i.name}
          </MenuItem>
        ))}
      </Select>
    </Box>
  );
};
