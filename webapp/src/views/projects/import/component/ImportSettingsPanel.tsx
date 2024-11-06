import React, { FC, useState } from 'react';
import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { LoadingCheckboxWithSkeleton } from 'tg.component/common/form/LoadingCheckboxWithSkeleton';
import { HelpCircle } from '@untitled-ui/icons-react';
import { DOC_LINKS } from '../../../../docLinks';

type ImportSettingRequest = components['schemas']['ImportSettingsRequest'];
type ImportSettingModel = components['schemas']['ImportSettingsModel'];

const StyledPanelBox = styled(Box)`
  margin-top: 24px;
  border: 1px solid ${({ theme }) => theme.palette.tokens.border.secondary};
  display: flex;
  width: 1200px;
  padding: 6px 16px;
  justify-content: center;
  align-items: center;
  gap: 20px;
  border-radius: 4px;
  background-color: ${({ theme }) =>
    theme.palette.tokens.background['paper-3']};
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
        color: theme.palette.tokens.text.primary,
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
          customHelpIcon={
            <StyledLink href={DOC_LINKS.importingPlaceholders}>
              <HelpCircle className="icon" />
            </StyledLink>
          }
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
        customHelpIcon={
          <StyledLink href={DOC_LINKS.importOverridingDescriptions}>
            <Box display="flex">
              <HelpCircle className="icon" />
            </Box>
          </StyledLink>
        }
        {...additionalCheckboxProps}
      />
      <LoadingCheckboxWithSkeleton
        loading={loadingItems.has('createNewKeys')}
        onChange={(e) => {
          onChange('createNewKeys', e.target.checked);
        }}
        data-cy={''}
        hint={t('import_only_update_without_add_key_label_hint')}
        label={t('import_only_update_without_add_key_label')}
        checked={state?.createNewKeys}
        {...additionalCheckboxProps}
      />
    </StyledPanelBox>
  );
};

const additionalCheckboxProps = {
  labelInnerProps: { sx: { fontSize: '15px' } },
  labelProps: { sx: { marginRight: 0 } },
};

const StyledLink = styled('a')`
  color: ${({ theme }) => theme.palette.tokens.icon.primary};

  .icon {
    width: 18px;
    height: 18px;
  }
`;
