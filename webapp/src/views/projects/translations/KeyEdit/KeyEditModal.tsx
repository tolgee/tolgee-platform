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

import { useProject } from 'tg.hooks/useProject';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useTranslationsActions } from '../context/TranslationsContext';
import { KeyGeneral } from './KeyGeneral';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useState } from 'react';
import { KeyAdvanced } from './KeyAdvanced';
import { KeyContext } from './KeyContext';

type TabsType = 'general' | 'advanced' | 'context';

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
  keyId: number;
  name: string;
  namespace: string | undefined;
  description: string | undefined;
  tags: string[];
  onClose: () => void;
  initialTab: TabsType;
  contextPresent: boolean;
};

export const KeyEditModal: React.FC<Props> = ({
  keyId,
  name,
  namespace = '',
  description,
  tags,
  onClose,
  initialTab,
  contextPresent,
}) => {
  const { t } = useTranslate();
  const project = useProject();
  const [tab, setTab] = useState<TabsType>(initialTab);
  const { updateKey } = useTranslationsActions();

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

  const initialValues = { name, namespace, description, tags, disabledLangs };

  return (
    <Formik
      initialValues={initialValues}
      enableReinitialize
      onSubmit={async (values, helpers) => {
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
                },
              },
            },
            {
              onError(e) {
                if (e.STANDARD_VALIDATION) {
                  helpers.setErrors(e.STANDARD_VALIDATION);
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
            },
          });
        } catch (e) {
          // eslint-disable-next-line no-console
          console.error(e);
        }
      }}
    >
      {({ submitForm }) => {
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
                {contextPresent && (
                  <StyledTab
                    data-cy="key-edit-tab-context"
                    value="context"
                    label={t('key_edit_modal_switch_context')}
                  />
                )}
              </StyledTabs>
            </StyledTabsWrapper>
            <StyledDialogContent>
              {tab === 'general' ? (
                <KeyGeneral />
              ) : tab === 'advanced' ? (
                <KeyAdvanced />
              ) : (
                <KeyContext keyId={keyId} />
              )}
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
              >
                <T keyName="global_form_save" />
              </LoadingButton>
            </DialogActions>
          </Dialog>
        );
      }}
    </Formik>
  );
};
