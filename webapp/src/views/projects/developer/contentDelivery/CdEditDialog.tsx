import { useMemo } from 'react';
import { Formik } from 'formik';
import { T, useTranslate } from '@tolgee/react';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  styled,
} from '@mui/material';

import { useProject } from 'tg.hooks/useProject';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { EXPORTABLE_STATES, StateType } from 'tg.constants/translationStates';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { StateSelector } from 'tg.views/projects/export/components/StateSelector';
import { LanguageSelector } from 'tg.views/projects/export/components/LanguageSelector';
import {
  FORMATS,
  FormatSelector,
} from 'tg.views/projects/export/components/FormatSelector';
import { NsSelector } from 'tg.views/projects/export/components/NsSelector';
import { NestedSelector } from 'tg.views/projects/export/components/NestedSelector';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { components } from 'tg.service/apiSchema.generated';
import { confirmation } from 'tg.hooks/confirmation';
import { TextField } from 'tg.component/common/form/fields/TextField';

import { CdStorageSelector } from './CdStorageSelector';
import { CdAutoPublish } from './CdAutoPublish';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { Validation } from 'tg.constants/GlobalValidationSchema';

type ContentDeliveryConfigModel =
  components['schemas']['ContentDeliveryConfigModel'];

const sortStates = (arr: StateType[]) =>
  [...arr].sort(
    (a, b) => EXPORTABLE_STATES.indexOf(a) - EXPORTABLE_STATES.indexOf(b)
  );

const EXPORT_DEFAULT_STATES: StateType[] = sortStates([
  'TRANSLATED',
  'REVIEWED',
]);

const EXPORT_DEFAULT_FORMAT: (typeof FORMATS)[number] = 'JSON';

const StyledDialogContent = styled(DialogContent)`
  display: grid;
  gap: ${({ theme }) => theme.spacing(3)};
  margin-top: 8px;
  grid-template-columns: 1fr 1fr;
  width: 85vw;
  max-width: 700px;
`;

const StyledOptions = styled('div')`
  display: grid;
  grid-column: 1 / span 2;
  gap: 8px;
  justify-items: start;
`;

type Props = {
  onClose: () => void;
  data?: ContentDeliveryConfigModel;
};

export const CdEditDialog = ({ onClose, data }: Props) => {
  const project = useProject();
  const { satisfiesLanguageAccess } = useProjectPermissions();
  const messaging = useMessage();

  const createCd = useApiMutation({
    url: '/v2/projects/{projectId}/content-delivery-configs',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/content-delivery-configs',
  });

  const updateCd = useApiMutation({
    url: '/v2/projects/{projectId}/content-delivery-configs/{id}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/content-delivery-configs',
  });

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
  });

  const namespacesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/used-namespaces',
    method: 'get',
    path: { projectId: project.id },
    fetchOptions: {
      disable404Redirect: true,
    },
  });

  const storagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/content-storages',
    method: 'get',
    path: { projectId: project.id },
    query: { page: 0, size: 1000 },
  });

  const { t } = useTranslate();

  const allNamespaces = useMemo(
    () =>
      namespacesLoadable.data?._embedded?.namespaces?.map((n) => n.name || ''),
    [namespacesLoadable.data]
  );

  const allowedLanguages = useMemo(
    () =>
      languagesLoadable.data?._embedded?.languages?.filter((l) =>
        satisfiesLanguageAccess('translations.view', l.id)
      ) || [],
    [languagesLoadable.data]
  );

  const allowedTags = useMemo(
    () => allowedLanguages?.map((l) => l.tag) || [],
    [allowedLanguages]
  );

  const deleteItem = useApiMutation({
    url: '/v2/projects/{projectId}/content-delivery-configs/{id}',
    method: 'delete',
    invalidatePrefix: '/v2/projects/{projectId}/content-delivery-configs',
  });

  function handleDelete() {
    confirmation({
      title: <T keyName="content_delivery_item_delete_dialog_title" />,
      onConfirm() {
        deleteItem.mutate(
          { path: { projectId: project.id, id: data!.id } },
          {
            onSuccess() {
              onClose();
              messaging.success(
                <T keyName="content_delivery_delete_success" />
              );
            },
          }
        );
      },
    });
  }

  return (
    <Dialog open onClose={onClose} maxWidth="lg">
      {languagesLoadable.isFetching || namespacesLoadable.isFetching ? (
        <Box m={4} justifyContent="center" display="flex">
          <SpinnerProgress />
        </Box>
      ) : (
        <Formik
          initialValues={{
            name: data?.name ?? '',
            states: data?.filterState ?? EXPORT_DEFAULT_STATES,
            languages: data?.languages ?? allowedTags,
            format: data?.format ?? EXPORT_DEFAULT_FORMAT,
            namespaces: allNamespaces ?? [],
            autoPublish: data?.autoPublish ?? true,
            nested: data?.structureDelimiter === '.',
            contentStorageId: data?.storage?.id,
          }}
          validationSchema={Validation.CONTENT_DELIVERY_FORM}
          validateOnBlur={false}
          enableReinitialize={false}
          onSubmit={(values, actions) => {
            if (data) {
              updateCd.mutate(
                {
                  path: { projectId: project.id, id: data!.id },
                  content: {
                    'application/json': {
                      name: values.name,
                      format: values.format,
                      filterState: values.states,
                      languages: values.languages,
                      structureDelimiter: values.nested ? '.' : '',
                      filterNamespace: undefinedIfAllNamespaces(
                        values.namespaces,
                        allNamespaces
                      ),
                      autoPublish: values.autoPublish,
                      contentStorageId: values.contentStorageId,
                    },
                  },
                },
                {
                  onSuccess() {
                    onClose();
                    messaging.success(
                      <T keyName="content_delivery_update_success" />
                    );
                  },
                  onSettled() {
                    actions.setSubmitting(false);
                  },
                }
              );
            } else {
              createCd.mutate(
                {
                  path: { projectId: project.id },
                  content: {
                    'application/json': {
                      name: values.name,
                      format: values.format,
                      filterState: values.states,
                      languages: values.languages,
                      structureDelimiter: values.nested ? '.' : '',
                      filterNamespace: undefinedIfAllNamespaces(
                        values.namespaces,
                        allNamespaces
                      ),
                      autoPublish: values.autoPublish,
                      contentStorageId: values.contentStorageId,
                    },
                  },
                },
                {
                  onSuccess() {
                    onClose();
                    messaging.success(
                      <T keyName="content_delivery_create_success" />
                    );
                  },
                  onSettled() {
                    actions.setSubmitting(false);
                  },
                }
              );
            }
          }}
        >
          {({ isSubmitting, handleSubmit, isValid, values }) => (
            <>
              <DialogTitle>
                {data
                  ? t('content_delivery_update_title')
                  : t('content_delivery_create_title')}
              </DialogTitle>
              <StyledDialogContent>
                <Box sx={{ gridColumn: '1 / span 2', display: 'grid' }}>
                  <TextField
                    name="name"
                    label={t('content_delivery_form_name_label')}
                    variant="standard"
                    data-cy="content-delivery-form-name"
                  />
                </Box>
                <Box sx={{ gridColumn: '1 / span 2', display: 'grid' }}>
                  <StateSelector className="states" />
                </Box>
                <LanguageSelector
                  className="langs"
                  languages={allowedLanguages}
                />
                <FormatSelector className="format" />
                <Box sx={{ gridColumn: '1 / span 2', display: 'grid' }}>
                  <NsSelector className="ns" namespaces={allNamespaces} />
                </Box>
                {Boolean(
                  storagesLoadable.data?._embedded?.contentStorages?.length
                ) && (
                  <Box sx={{ gridColumn: '1 / span 2', display: 'grid' }}>
                    <CdStorageSelector
                      name="contentStorageId"
                      label={t('content_delivery_form_storage')}
                      items={
                        storagesLoadable.data?._embedded?.contentStorages || []
                      }
                    />
                  </Box>
                )}

                <StyledOptions>
                  {values.format === 'JSON' && <NestedSelector />}
                  <CdAutoPublish />
                </StyledOptions>
              </StyledDialogContent>
              <DialogActions sx={{ justifyContent: 'space-between' }}>
                <div>
                  {data && (
                    <Button
                      onClick={handleDelete}
                      variant="outlined"
                      data-cy="content-delivery-delete-button"
                    >
                      {t('content_delivery_form_delete')}
                    </Button>
                  )}
                </div>
                <Box display="flex" gap={1}>
                  <Button onClick={onClose}>
                    {t('content_delivery_form_cancel')}
                  </Button>
                  <LoadingButton
                    data-cy="content-delivery-form-save"
                    loading={isSubmitting}
                    variant="contained"
                    color="primary"
                    onClick={() => handleSubmit()}
                  >
                    {t('content_delivery_form_save')}
                  </LoadingButton>
                </Box>
              </DialogActions>
            </>
          )}
        </Formik>
      )}
    </Dialog>
  );
};

function undefinedIfAllNamespaces(
  selectedNamespaces: string[],
  allNamespaces: string[] | undefined
) {
  if (!allNamespaces) {
    return selectedNamespaces;
  }
  if (selectedNamespaces.length === allNamespaces.length) {
    return undefined;
  }
  return selectedNamespaces;
}
