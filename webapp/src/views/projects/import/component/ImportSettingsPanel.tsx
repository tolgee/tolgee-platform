import React, { FC, useState } from 'react';
import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { LoadingCheckboxWithSkeleton } from 'tg.component/common/form/LoadingCheckboxWithSkeleton';

type ImportSettingRequest = components['schemas']['ImportSettingsRequest'];
type ImportSettingModel = components['schemas']['ImportSettingsModel'];

const StyledPanelBox = styled(Box)`
  margin-top: 24px;
  border: 1px solid ${({ theme }) => theme.palette.tokens.BORDER_SECONDARY};
  display: flex;
  width: 1200px;
  padding: 6px 16px;
  justify-content: center;
  align-items: center;
  gap: 20px;
  border-radius: 4px;
  background-color: ${({ theme }) =>
    theme.palette.tokens.SURFACE_BACKGROUND_SECONDARY};
`;

export const ImportSettingsPanel: FC = (props) => {
  const project = useProject();
  const { t } = useTranslate();

  const [state, setState] = useState<ImportSettingRequest | undefined>(
    undefined
  );

  const [loadingItems, setLoadingItems] = useState<
    Set<keyof ImportSettingRequest>
  >(new Set());

  useApiQuery({
    url: '/v2/projects/{projectId}/import-settings',
    method: 'get',
    path: { projectId: project.id },
    options: {
      onSuccess: (data) => {
        setState(data);
      },
    },
  });

  const updateSettings = useApiMutation({
    url: '/v2/projects/{projectId}/import-settings',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/import',
  });

  function onChange<T extends keyof ImportSettingRequest>(
    item: T,
    value: ImportSettingRequest[T]
  ) {
    if (state == undefined) {
      return;
    }
    const onSuccess = (data: ImportSettingModel) => {
      setState(data);
    };

    const onSettled = () => {
      setLoadingItems((loadingItems) => {
        const copy = new Set([...loadingItems]);
        copy.delete(item);
        return copy;
      });
    };

    const newValue = { ...state, [item]: value };
    setLoadingItems((loadingItems) => {
      const copy = new Set([...loadingItems]);
      copy.add(item);
      return copy;
    });
    updateSettings.mutate(
      {
        path: { projectId: project.id },
        content: { 'application/json': newValue },
      },
      {
        onSuccess,
        onSettled,
      }
    );
    return;
  }

  return (
    <StyledPanelBox
      sx={(theme) => ({
        color: theme.palette.tokens.TEXT_PRIMARY,
      })}
    >
      {project.icuPlaceholders && (
        <LoadingCheckboxWithSkeleton
          loading={loadingItems.has('convertPlaceholdersToIcu')}
          onChange={(e) => {
            onChange('convertPlaceholdersToIcu', e.target.checked);
          }}
          data-cy={'import-convert-placeholders-to-icu-checkbox'}
          hint={t('import_convert_placeholders_to_icu_checkbox_label_hint')}
          label={t('import_convert_placeholders_to_icu_checkbox_label')}
          checked={state?.convertPlaceholdersToIcu}
          {...additionalCheckboxProps}
        />
      )}
      <LoadingCheckboxWithSkeleton
        loading={loadingItems.has('overrideKeyDescriptions')}
        onChange={(e) => {
          onChange('overrideKeyDescriptions', e.target.checked);
        }}
        data-cy={'import-override-key-descriptions-checkbox'}
        hint={t('import_override_key_descriptions_label_hint')}
        label={t('import_override_key_descriptions_label')}
        checked={state?.overrideKeyDescriptions}
        {...additionalCheckboxProps}
      />
    </StyledPanelBox>
  );
};

const additionalCheckboxProps = {
  labelInnerProps: { sx: { fontSize: '15px' } },
  labelProps: { sx: { marginRight: 0 } },
};
