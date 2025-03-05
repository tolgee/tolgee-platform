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
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { StateSelector } from 'tg.views/projects/export/components/StateSelector';
import { LanguageSelector } from 'tg.views/projects/export/components/LanguageSelector';
import { FormatSelector } from 'tg.views/projects/export/components/FormatSelector';
import { NsSelector } from 'tg.views/projects/export/components/NsSelector';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { confirmation } from 'tg.hooks/confirmation';
import { TextField } from 'tg.component/common/form/fields/TextField';

import { CdStorageSelector } from './CdStorageSelector';
import { CdAutoPublish } from './CdAutoPublish';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { getFormatById } from '../../export/components/formatGroups';
import { SupportArraysSelector } from '../../export/components/SupportArraysSelector';
import { MessageFormatSelector } from '../../export/components/MessageFormatSelector';
import {
  ContentDeliveryConfigModel,
  getCdEditInitialValues,
} from './getCdEditInitialValues';
import { useCdActions } from './useCdActions';
import { useExportHelper } from 'tg.hooks/useExportHelper';
import { CdPruneBeforePublish } from './CdPruneBeforePublish';
import { EscapeHtmlSelector } from 'tg.views/projects/export/components/EscapeHtmlSelector';

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

export const CdDialog = ({ onClose, data }: Props) => {
  const project = useProject();
  const messaging = useMessage();

  const storagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/content-storages',
    method: 'get',
    path: { projectId: project.id },
    query: { page: 0, size: 1000 },
  });

  const { t } = useTranslate();

  const deleteItem = useApiMutation({
    url: '/v2/projects/{projectId}/content-delivery-configs/{id}',
    method: 'delete',
    invalidatePrefix: '/v2/projects/{projectId}/content-delivery-configs',
  });

  const { isFetching, allowedLanguageTags, allNamespaces, allowedLanguages } =
    useExportHelper();

  const actions = useCdActions({ allNamespaces, onClose });

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
    <Dialog open={true} onClose={onClose} maxWidth="lg">
      {isFetching ? (
        <Box m={4} justifyContent="center" display="flex">
          <SpinnerProgress />
        </Box>
      ) : (
        <Formik
          initialValues={getCdEditInitialValues(
            data,
            allowedLanguageTags,
            allNamespaces
          )}
          validationSchema={Validation.CONTENT_DELIVERY_FORM}
          validateOnBlur={false}
          enableReinitialize={false}
          onSubmit={(values, formikHelpers) => {
            if (data) {
              return actions.update(values, formikHelpers, data.id);
            }
            return actions.create(values, formikHelpers);
          }}
        >
          {({ isSubmitting, handleSubmit, isValid, values }) => {
            return (
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
                      minHeight={false}
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
                  <NsSelector className="ns" namespaces={allNamespaces} />
                  <MessageFormatSelector className="messageFormat" />
                  {Boolean(
                    storagesLoadable.data?._embedded?.contentStorages?.length
                  ) && (
                    <Box sx={{ gridColumn: '1 / span 2', display: 'grid' }}>
                      <CdStorageSelector
                        items={
                          storagesLoadable.data?._embedded?.contentStorages ||
                          []
                        }
                      />
                    </Box>
                  )}

                  <StyledOptions>
                    {getFormatById(values.format).showSupportArrays && (
                      <SupportArraysSelector />
                    )}
                    <CdAutoPublish />
                    <CdPruneBeforePublish />
                    {getFormatById(values.format).showEscapeHtml && (
                      <EscapeHtmlSelector />
                    )}
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
            );
          }}
        </Formik>
      )}
    </Dialog>
  );
};
