import { Box, Button } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useRouteMatch } from 'react-router-dom';
import { container } from 'tsyringe';

import { ConfirmationDialogProps } from 'tg.component/common/ConfirmationDialog';
import { LanguageModifyFields } from 'tg.component/languages/LanguageModifyFields';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS, PARAMS } from 'tg.constants/links';
import { confirmation } from 'tg.hooks/confirmation';
import { redirect } from 'tg.hooks/redirect';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { BaseProjectView } from 'tg.views/projects/BaseProjectView';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';

type LanguageModel = components['schemas']['LanguageModel'];

const messageService = container.resolve(MessageService);

export const LanguageEditView = () => {
  const confirmationMessage = (options: ConfirmationDialogProps) =>
    confirmation({ title: 'Delete language', ...options });
  const { refetchUsage } = useGlobalActions();

  const match = useRouteMatch();
  const project = useProject();
  const { t } = useTranslate();

  const projectId = match.params[PARAMS.PROJECT_ID];
  const languageId = match.params[PARAMS.LANGUAGE_ID] as number;

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
  });
  const deleteLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/languages/{languageId}',
    method: 'delete',
  });

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
          redirect(LINKS.PROJECT_LANGUAGES, {
            [PARAMS.PROJECT_ID]: projectId,
          });
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
          redirect(LINKS.PROJECT_LANGUAGES, {
            [PARAMS.PROJECT_ID]: projectId,
          });
        },
      }
    );
  };

  return (
    <BaseProjectView
      maxWidth="narrow"
      windowTitle={t('language_settings_title')}
      navigation={[
        [
          t('project_menu_languages'),
          LINKS.PROJECT_LANGUAGES.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
        [
          t('language_settings_title'),
          LINKS.PROJECT_EDIT_LANGUAGE.build({
            [PARAMS.PROJECT_ID]: project.id,
            [PARAMS.LANGUAGE_ID]: languageId,
          }),
        ],
      ]}
      loading={
        languageLoadable.isFetching ||
        editLoadable.isLoading ||
        deleteLoadable.isLoading
      }
      hideChildrenOnLoading={languageLoadable.isLoading}
    >
      <StandardForm
        initialValues={{
          ...languageLoadable.data!,
          flagEmoji: languageLoadable.data?.flagEmoji || '🏳️',
          originalName: languageLoadable.data?.originalName || '',
        }}
        onSubmit={onSubmit}
        saveActionLoadable={editLoadable}
        validationSchema={Validation.LANGUAGE(t)}
        customActions={
          <Button
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
          </Button>
        }
      >
        <Box data-cy="language-modify-form">
          <LanguageModifyFields />
        </Box>
      </StandardForm>
    </BaseProjectView>
  );
};
