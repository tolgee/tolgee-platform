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

import { Box, MenuItem, styled } from '@mui/material';
import { useLLMProviderTranslation } from 'tg.translationTools/useLLMProviderTranslation';
import { LabelHint } from 'tg.component/common/LabelHint';

const StyledEmpty = styled(Box)`
  font-style: italic;
`;

type Props = {
  type: LlmProviderType;
  onTypeChange: (value: LlmProviderType) => void;
};

export const LLMProviderForm = ({ type, onTypeChange }: Props) => {
  const { t } = useTranslate();
  const defaults = llmProvidersDefaults(t);
  const globalConfig = llmProvidersConfig(t);
  const config = globalConfig[type];
  const translateType = useLLMProviderTranslation();
  return (
    <>
      <MuiSelect
        label={t('llm_provider_form_type')}
        value={type}
        onChange={(e) => onTypeChange(e.target.value as LlmProviderType)}
        size="small"
      >
        {(Object.keys(globalConfig) as LlmProviderType[]).map((type) => (
          <MenuItem key={type} value={type}>
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
          return <TextField key={name} name={name} label={labelWithHint} />;
        }
      })}
      <Select
        name="priority"
        label={
          <LabelHint title={t('llm_provider_form_priority_hint')}>
            {t('llm_provider_form_priority')}
          </LabelHint>
        }
        displayEmpty
      >
        <MenuItem value={undefined}>
          <StyledEmpty>
            <T keyName="llm_provider_form_select_priority_none" />
          </StyledEmpty>
        </MenuItem>
        <MenuItem value="high">
          <T keyName="llm_provider_form_select_priority_high" />
        </MenuItem>
        <MenuItem value="low">
          <T keyName="llm_provider_form_select_priority_low" />
        </MenuItem>
      </Select>
    </>
  );
};
