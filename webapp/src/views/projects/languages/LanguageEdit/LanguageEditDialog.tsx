import { Box, Dialog, DialogContent, DialogTitle } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useHistory, useRouteMatch } from 'react-router-dom';

import { ConfirmationDialogProps } from 'tg.component/common/ConfirmationDialog';
import { LanguageModifyFields } from 'tg.component/languages/LanguageModifyFields';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS, PARAMS } from 'tg.constants/links';
import { confirmation } from 'tg.hooks/confirmation';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { messageService } from 'tg.service/MessageService';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import LoadingButton from 'tg.component/common/form/LoadingButton';

type LanguageModel = components['schemas']['LanguageModel'];

export const LanguageEditDialog = () => {
  const confirmationMessage = (options: ConfirmationDialogProps) =>
    confirmation({ title: 'Delete language', ...options });
  const { refetchUsage } = useGlobalActions();

  const history = useHistory();

  const match = useRouteMatch(LINKS.PROJECT_EDIT_LANGUAGE.template);
  const { t } = useTranslate();

  const projectId = match?.params[PARAMS.PROJECT_ID];
  const languageId = match?.params[PARAMS.LANGUAGE_ID] as number;

  const languageLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages/{languageId}',
    method: 'get',
    path: { projectId, languageId },
    options: {
      cacheTime: 0,
    },
  });
  const editLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/languages/{languageId}',
    method: 'put',
    invalidatePrefix: '/v2/projects',
  });
  const deleteLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/languages/{languageId}',
    method: 'delete',
    invalidatePrefix: '/v2/projects',
  });

  const onClose = () => {
    history.push(
      LINKS.PROJECT_LANGUAGES.build({ [PARAMS.PROJECT_ID]: projectId })
    );
  };

  const onSubmit = (values: LanguageModel) => {
    const { name, originalName, flagEmoji, tag } = values;
    editLoadable.mutate(
      {
        path: {
          projectId: projectId,
          languageId: languageId,
        },
        content: {
          'application/json': {
            name,
            originalName: originalName as string,
            tag,
            flagEmoji,
          },
        },
      },
      {
        onSuccess() {
          messageService.success(<T keyName="language_edited_message" />);
          history.push(
            LINKS.PROJECT_LANGUAGES.build({
              [PARAMS.PROJECT_ID]: projectId,
            })
          );
        },
      }
    );
  };

  const onDelete = () => {
    deleteLoadable.mutate(
      {
        path: {
          projectId: projectId,
          languageId: languageId,
        },
      },
      {
        onSuccess() {
          refetchUsage();
          messageService.success(<T keyName="language_deleted_message" />);
          history.push(
            LINKS.PROJECT_LANGUAGES.build({
              [PARAMS.PROJECT_ID]: projectId,
            })
          );
        },
      }
    );
  };

  return (
    <Dialog open onClose={onClose}>
      <DialogTitle>{t('language_settings_title')}</DialogTitle>
      <DialogContent sx={{ width: 500, maxWidth: '100%' }}>
        {languageLoadable.isLoading ? (
          <BoxLoading />
        ) : (
          <StandardForm
            initialValues={{
              ...languageLoadable.data!,
              flagEmoji: languageLoadable.data?.flagEmoji || '🏳️',
              originalName: languageLoadable.data?.originalName || '',
            }}
            onCancel={onClose}
            onSubmit={onSubmit}
            saveActionLoadable={editLoadable}
            validationSchema={Validation.LANGUAGE(t)}
            customActions={
              <LoadingButton
                loading={deleteLoadable.isLoading}
                variant="outlined"
                color="secondary"
                data-cy="language-delete-button"
                onClick={() => {
                  if (languageLoadable.data?.base) {
                    return messageService.error(
                      <T keyName="cannot_delete_base_language_message" />
                    );
                  }
                  confirmationMessage({
                    message: (
                      <T
                        keyName="delete_language_confirmation"
                        params={{ name: languageLoadable.data!.name }}
                      />
                    ),
                    hardModeText: languageLoadable.data!.name.toUpperCase(),
                    confirmButtonText: <T keyName="global_delete_button" />,
                    confirmButtonColor: 'secondary',
                    onConfirm: onDelete,
                  });
                }}
              >
                <T keyName="delete_language_button" />
              </LoadingButton>
            }
          >
            <Box data-cy="language-modify-form">
              <LanguageModifyFields />
            </Box>
          </StandardForm>
        )}
      </DialogContent>
    </Dialog>
  );
};
