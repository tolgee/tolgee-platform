import {
  Dialog,
  DialogTitle,
  DialogActions,
  DialogContent,
  Tabs,
  Tab,
  styled,
} from '@mui/material';
import { useTranslate, T } from '@tolgee/react';
import { Formik } from 'formik';
import { Button } from '@mui/material';
import { getFirstPluralParameter } from '@tginternal/editor';

import { useProject } from 'tg.hooks/useProject';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useTranslationsActions } from '../context/TranslationsContext';
import { KeyGeneral } from './KeyGeneral';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useMemo, useState } from 'react';
import { KeyAdvanced } from './KeyAdvanced';
import { KeyContext } from './KeyContext';
import { KeyFormType } from './types';
import { KeyCustomValues } from './KeyCustomValues';
import { DeletableKeyWithTranslationsModelType } from '../context/types';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import ConfirmationDialog from 'tg.component/common/ConfirmationDialog';

type TabsType = 'general' | 'advanced' | 'context' | 'customValues';

const StyledDialogContent = styled(DialogContent)`
  display: grid;
  row-gap: ${({ theme }) => theme.spacing(2)};
  margin-bottom: ${({ theme }) => theme.spacing(2)};
  justify-content: stretch;
  max-width: 100%;
  position: relative;
  align-content: start;
  min-height: 350px;
`;

const StyledTabsWrapper = styled('div')`
  display: flex;
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
  justify-content: space-between;
  align-items: center;
  margin: 0px 24px;
`;

const StyledTabs = styled(Tabs)`
  min-height: 0px;
  margin-bottom: -1px;
`;

const StyledTab = styled(Tab)`
  min-height: 0px;
  min-width: 60px;
  margin: 0px 0px;
  padding: 9px 12px;
`;

type Props = {
  data: DeletableKeyWithTranslationsModelType;
  onClose: () => void;
  initialTab: TabsType;
};

export const KeyEditModal: React.FC<Props> = ({
  onClose,
  initialTab,
  data,
}) => {
  const { t } = useTranslate();
  const project = useProject();
  const [tab, setTab] = useState<TabsType>(initialTab);
  const { updateKey } = useTranslationsActions();
  const keyId = data.keyId;
  const [warningOpen, setWarningOpen] = useState(false);

  const keyInfoLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/keys/{id}',
    method: 'get',
    path: { projectId: project.id, id: keyId },
  });

  const customValues = keyInfoLoadable.data?.custom;
  const updateKeyLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/keys/{id}/complex-update',
    method: 'put',
  });

  const disabledLangsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/keys/{id}/disabled-languages',
    method: 'get',
    path: { projectId: project.id, id: keyId },
  });

  const disableLangsLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/keys/{id}/disabled-languages',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/keys/{id}/disabled-languages',
  });

  const disabledLangs =
    disabledLangsLoadable.data?._embedded?.languages?.map((l) => l.id) || [];

  const extractedArgName = useMemo(() => {
    // try to extract parameter name from base language translation
    if (project.baseLanguage?.tag) {
      return getFirstPluralParameter(
        data.translations?.[project.baseLanguage.tag]?.text ?? ''
      );
    }
  }, [data]);

  const initialValues = {
    name: data.keyName,
    namespace: data.keyNamespace ?? '',
    description: data.keyDescription,
    tags: data.keyTags.map((t) => t.name),
    disabledLangs,
    isPlural: data.keyIsPlural,
    pluralParameter:
      (data.keyIsPlural ? data.keyPluralArgName : undefined) ||
      extractedArgName ||
      'value',
    custom: JSON.stringify(customValues ?? {}, null, 2),
  } satisfies KeyFormType;

  return (
    <Formik
      initialValues={initialValues}
      enableReinitialize
      validationSchema={Validation.KEY_SETTINGS_FORM(t)}
      onSubmit={async (values, helpers) => {
        const custom = JSON.parse(values.custom);
        try {
          const data = await updateKeyLoadable.mutateAsync(
            {
              path: { projectId: project.id, id: keyId },
              content: {
                'application/json': {
                  name: values.name,
                  namespace: values.namespace,
                  tags: values.tags,
                  description: values.description,
                  isPlural: values.isPlural,
                  pluralArgName: values.pluralParameter,
                  custom,
                  warnOnDataLoss: !warningOpen,
                },
              },
            },
            {
              onError(e) {
                if (e.STANDARD_VALIDATION) {
                  helpers.setErrors(e.STANDARD_VALIDATION);
                } else if (e.code === 'plural_forms_data_loss') {
                  setWarningOpen(true);
                } else {
                  e.handleError?.();
                }
              },
            }
          );

          await disableLangsLoadable.mutateAsync({
            path: { projectId: project.id, id: keyId },
            content: {
              'application/json': { languageIds: values.disabledLangs },
            },
          });

          onClose();
          updateKey({
            keyId,
            value: {
              keyName: data.name,
              keyNamespace: data.namespace,
              keyDescription: data.description,
              keyTags: data.tags,
              keyIsPlural: data.isPlural,
              keyPluralArgName: data.pluralArgName,
            },
          });
        } catch (e) {
          // eslint-disable-next-line no-console
          console.error(e);
        }
      }}
    >
      {({ submitForm, isValid }) => {
        return (
          <Dialog open={true} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle>{t('translations_key_edit_title')}</DialogTitle>
            <StyledTabsWrapper>
              <StyledTabs value={tab} onChange={(_, val) => setTab(val)}>
                <StyledTab
                  data-cy="key-edit-tab-general"
                  value="general"
                  label={t('key_edit_modal_switch_general')}
                />
                <StyledTab
                  data-cy="key-edit-tab-advanced"
                  value="advanced"
                  label={t('key_edit_modal_switch_advanced')}
                />
                {data.contextPresent && (
                  <StyledTab
                    data-cy="key-edit-tab-context"
                    value="context"
                    label={t('key_edit_modal_switch_context')}
                  />
                )}
                <StyledTab
                  data-cy="key-edit-tab-custom-properties"
                  value="customValues"
                  label={t('key_edit_modeal_switch_custom_properties')}
                />
              </StyledTabs>
            </StyledTabsWrapper>
            <StyledDialogContent>
              {tab === 'general' ? (
                <KeyGeneral />
              ) : tab === 'advanced' ? (
                <KeyAdvanced />
              ) : tab === 'context' ? (
                <KeyContext keyId={keyId} />
              ) : tab === 'customValues' ? (
                <KeyCustomValues />
              ) : null}
            </StyledDialogContent>

            <DialogActions>
              <Button
                data-cy="translations-cell-cancel-button"
                onClick={onClose}
              >
                <T keyName="global_cancel_button" />
              </Button>
              <LoadingButton
                data-cy="translations-cell-save-button"
                loading={updateKeyLoadable.isLoading}
                color="primary"
                variant="contained"
                type="submit"
                onClick={() => submitForm()}
                disabled={!isValid}
              >
                <T keyName="global_form_save" />
              </LoadingButton>
            </DialogActions>
            {warningOpen && (
              <ConfirmationDialog
                title={t('key_edit_modal_force_plural_change_title')}
                message={t('key_edit_modal_force_plural_change_message')}
                onCancel={() => setWarningOpen(false)}
                onConfirm={() => submitForm()}
              />
            )}
          </Dialog>
        );
      }}
    </Formik>
  );
};
