import { container } from 'tsyringe';
import { LINKS, PARAMS } from '../../../constants/links';
import { useRouteMatch } from 'react-router-dom';
import { BaseFormView } from '../../../component/layout/BaseFormView';
import { Box, Button } from '@material-ui/core';
import { confirmation } from '../../../hooks/confirmation';
import { Validation } from '../../../constants/GlobalValidationSchema';
import { useRedirect } from '../../../hooks/useRedirect';
import { T } from '@tolgee/react';
import { ConfirmationDialogProps } from '../../../component/common/ConfirmationDialog';
import { LanguageModifyFields } from '../../../component/languages/LanguageModifyFields';
import { components } from '../../../service/apiSchema.generated';
import { MessageService } from '../../../service/MessageService';
import { useApiMutation, useApiQuery } from '../../../service/http/useQueryApi';

type LanguageModel = components['schemas']['LanguageModel'];

const messageService = container.resolve(MessageService);

export const LanguageEditView = () => {
  const confirmationMessage = (options: ConfirmationDialogProps) =>
    confirmation({ title: 'Delete language', ...options });

  const match = useRouteMatch();

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
          messageService.success(<T>language_edited_message</T>);
          useRedirect(LINKS.PROJECT_EDIT, {
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
          messageService.success(<T>language_deleted_message</T>);
          useRedirect(LINKS.PROJECT_EDIT, {
            [PARAMS.PROJECT_ID]: projectId,
          });
        },
      }
    );
  };

  return (
    <BaseFormView
      lg={6}
      md={8}
      xs={10}
      title={<T>language_settings_title</T>}
      initialValues={{
        ...languageLoadable.data!,
        flagEmoji: languageLoadable.data?.flagEmoji || '🏁',
        originalName: languageLoadable.data?.originalName || '',
      }}
      onSubmit={onSubmit}
      loading={
        languageLoadable.isFetching ||
        editLoadable.isLoading ||
        deleteLoadable.isLoading
      }
      hideChildrenOnLoading={languageLoadable.isLoading}
      validationSchema={Validation.LANGUAGE}
      customActions={
        <Button
          variant="outlined"
          color="secondary"
          data-cy="language-delete-button"
          onClick={() => {
            if (languageLoadable.data?.base) {
              return messageService.error(
                <T>cannot_delete_base_language_message</T>
              );
            }
            confirmationMessage({
              message: (
                <T parameters={{ name: languageLoadable.data!.name }}>
                  delete_language_confirmation
                </T>
              ),
              hardModeText: languageLoadable.data!.name.toUpperCase(),
              confirmButtonText: <T>global_delete_button</T>,
              confirmButtonColor: 'secondary',
              onConfirm: onDelete,
            });
          }}
        >
          <T>delete_language_button</T>
        </Button>
      }
    >
      <Box data-cy="language-modify-form">
        <LanguageModifyFields />
      </Box>
    </BaseFormView>
  );
};
