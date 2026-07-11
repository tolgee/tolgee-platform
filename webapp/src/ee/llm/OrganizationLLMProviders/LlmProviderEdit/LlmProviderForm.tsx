import { T, useTranslate } from '@tolgee/react';
import { TextField } from 'tg.component/common/form/fields/TextField';
import {
  llmProvidersConfig,
  llmProvidersDefaults,
  LlmProviderType,
  ProviderOptions,
} from './llmProvidersConfig';
import { Select } from 'tg.component/common/form/fields/Select';
import { Select as MuiSelect } from 'tg.component/common/Select';

import { Box, MenuItem, styled, useTheme } from '@mui/material';
import { useLlmProviderTranslation } from 'tg.translationTools/useLlmProviderTranslation';
import { LabelHint } from 'tg.component/common/LabelHint';

const StyledEmpty = styled(Box)`
  font-style: italic;
`;

type Props = {
  type: LlmProviderType;
  onTypeChange: (value: LlmProviderType) => void;
};

export const LlmProviderForm = ({ type, onTypeChange }: Props) => {
  const theme = useTheme();
  const { t } = useTranslate();
  const defaults = llmProvidersDefaults(t);
  const globalConfig = llmProvidersConfig(t);
  const config = globalConfig[type];
  const translateType = useLlmProviderTranslation();
  return (
    <>
      <MuiSelect
        label={t('llm_provider_form_type')}
        data-cy="llm-provider-form-type-select"
        value={type}
        onChange={(e) => onTypeChange(e.target.value as LlmProviderType)}
        size="small"
      >
        {(Object.keys(globalConfig) as LlmProviderType[]).map((type) => (
          <MenuItem
            key={type}
            value={type}
            data-cy="llm-provider-form-type-select-item"
          >
            {translateType(type)}
          </MenuItem>
        ))}
      </MuiSelect>
      {Object.entries(config).map(([name, o]) => {
        const options = { ...defaults[name], ...o } as ProviderOptions;
        const labelWithHint = options.hint ? (
          <LabelHint title={options.hint}>{options.label}</LabelHint>
        ) : (
          options.label
        );

        if (options.enum) {
          return (
            <Select
              key={name}
              name={name}
              label={labelWithHint}
              displayEmpty={true}
              data-cy="llm-provider-form-select"
              data-cy-name={name}
            >
              {options.enum.map((i) => (
                <MenuItem key={i} value={i}>
                  {i === undefined ? (
                    <StyledEmpty>
                      <T keyName="llm_provider_form_select_empty" />
                    </StyledEmpty>
                  ) : (
                    i
                  )}
                </MenuItem>
              ))}
            </Select>
          );
        } else {
          return (
            <TextField
              key={name}
              name={name}
              label={labelWithHint}
              data-cy="llm-provider-form-text-field"
              data-cy-name={name}
            />
          );
        }
      })}

      <Box
        gridColumn="1 / -1"
        height="1px"
        sx={{ backgroundColor: theme.palette.divider }}
        mb={2}
      />
      <Box>
        <Select
          name="priority"
          label={
            <LabelHint title={t('llm_provider_form_priority_hint')}>
              {t('llm_provider_form_priority')}
            </LabelHint>
          }
          data-cy="llm-provider-form-priority-select"
          displayEmpty
        >
          <MenuItem
            value={undefined}
            data-cy="llm-provider-form-priority-select-item"
          >
            <StyledEmpty>
              <T keyName="llm_provider_form_select_priority_none" />
            </StyledEmpty>
          </MenuItem>
          <MenuItem
            value="HIGH"
            data-cy="llm-provider-form-priority-select-item"
          >
            <T keyName="llm_provider_form_select_priority_high" />
          </MenuItem>
          <MenuItem
            value="LOW"
            data-cy="llm-provider-form-priority-select-item"
          >
            <T keyName="llm_provider_form_select_priority_low" />
          </MenuItem>
        </Select>
      </Box>
    </>
  );
};
