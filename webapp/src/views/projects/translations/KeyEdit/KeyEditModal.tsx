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
import { AppIcon } from '../../apps/AppIcon';

import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useTranslationsActions } from '../context/TranslationsContext';
import { KeyGeneral } from './KeyGeneral';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useMemo, useState } from 'react';
import { KeyAdvanced } from './KeyAdvanced';
import { KeyContext } from './KeyContext';
import { KeyFormType } from './types';
import { KeyCustomValues } from './KeyCustomValues';
import { KeyEditAppTab } from './KeyEditAppTab';
import { DeletableKeyWithTranslationsModelType } from '../context/types';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import ConfirmationDialog from 'tg.component/common/ConfirmationDialog';

type NativeTabId = 'general' | 'advanced' | 'context' | 'customValues';
type AppTabId = `app:${number}:${string}`;
export type TabsType = NativeTabId | AppTabId;

const isAppTab = (tab: TabsType): tab is AppTabId =>
  typeof tab === 'string' && tab.startsWith('app:');

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

export const KeyEditModal: React.FC<React.PropsWithChildren<Props>> = ({
  onClose,
  initialTab,
  data,
}) => {
  const { t } = useTranslate();
  const project = useProject();
  const projectPermissions = useProjectPermissions();
  const canEdit = projectPermissions.satisfiesPermission('keys.edit');
  const [tab, setTab] = useState<TabsType>(initialTab);
  const { updateKey } = useTranslationsActions();
  const keyId = data.keyId;
  const [warningOpen, setWarningOpen] = useState(false);

  const apps = useApiQuery({
    url: '/v2/projects/{projectId}/apps',
    method: 'get',
    path: { projectId: project.id },
    options: { staleTime: 60_000 },
  });

  const appTabs = useMemo(() => {
    const installs = apps.data?._embedded?.projectApps ?? [];
    const out: Array<{
      installId: number;
      baseUrl: string;
      tabKey: string;
      title: string;
      icon: string;
      entry: string;
    }> = [];
    for (const install of installs) {
      if (!install.enabled) continue;
      const tabs = install.modules?.['key-edit-tab'] ?? [];
      for (const m of tabs) {
        out.push({
          installId: install.id,
          baseUrl: install.baseUrl,
          tabKey: m.key,
          title: m.title,
          icon: m.icon,
          entry: m.entry,
        });
      }
    }
    return out;
  }, [apps.data]);

  const activeAppTab = useMemo(() => {
    if (!isAppTab(tab)) return null;
    const parts = tab.slice('app:'.length).split(':');
    const installId = Number(parts[0]);
    const tabKey = parts.slice(1).join(':');
    return (
      appTabs.find((t) => t.installId === installId && t.tabKey === tabKey) ??
      null
    );
  }, [tab, appTabs]);

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
    maxCharLimit: keyInfoLoadable.data?.maxCharLimit ?? undefined,
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
                  maxCharLimit:
                    values.maxCharLimit != null ? values.maxCharLimit : 0,
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
              keyMaxCharLimit: values.maxCharLimit ?? undefined,
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
                {canEdit && (
                  <StyledTab
                    data-cy="key-edit-tab-general"
                    value="general"
                    label={t('key_edit_modal_switch_general')}
                  />
                )}
                {canEdit && (
                  <StyledTab
                    data-cy="key-edit-tab-advanced"
                    value="advanced"
                    label={t('key_edit_modal_switch_advanced')}
                  />
                )}
                {canEdit && data.contextPresent && (
                  <StyledTab
                    data-cy="key-edit-tab-context"
                    value="context"
                    label={t('key_edit_modal_switch_context')}
                  />
                )}
                {canEdit && (
                  <StyledTab
                    data-cy="key-edit-tab-custom-properties"
                    value="customValues"
                    label={t('key_edit_modeal_switch_custom_properties')}
                  />
                )}
                {appTabs.map((appTab) => (
                  <StyledTab
                    key={`app:${appTab.installId}:${appTab.tabKey}`}
                    data-cy="key-edit-tab-app"
                    data-cy-install-id={appTab.installId}
                    data-cy-tab-key={appTab.tabKey}
                    value={`app:${appTab.installId}:${appTab.tabKey}`}
                    label={
                      <span
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: 6,
                        }}
                      >
                        <AppIcon icon={appTab.icon} size={16} />
                        {appTab.title}
                      </span>
                    }
                  />
                ))}
              </StyledTabs>
            </StyledTabsWrapper>
            <StyledDialogContent>
              {tab === 'general' && canEdit ? (
                <KeyGeneral />
              ) : tab === 'advanced' && canEdit ? (
                <KeyAdvanced />
              ) : tab === 'context' && canEdit ? (
                <KeyContext keyId={keyId} />
              ) : tab === 'customValues' && canEdit ? (
                <KeyCustomValues />
              ) : activeAppTab ? (
                <KeyEditAppTab
                  installId={activeAppTab.installId}
                  baseUrl={activeAppTab.baseUrl}
                  entry={activeAppTab.entry}
                  data={data}
                />
              ) : null}
            </StyledDialogContent>

            <DialogActions>
              <Button
                data-cy="translations-cell-cancel-button"
                onClick={onClose}
              >
                <T keyName="global_cancel_button" />
              </Button>
              {canEdit && (
                <LoadingButton
                  data-cy="translations-cell-main-action-button"
                  loading={updateKeyLoadable.isLoading}
                  color="primary"
                  variant="contained"
                  type="submit"
                  onClick={() => submitForm()}
                  disabled={!isValid}
                >
                  <T keyName="global_form_save" />
                </LoadingButton>
              )}
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
