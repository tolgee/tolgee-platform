import * as React from 'react';
import { useEffect } from 'react';
import { container } from 'tsyringe';
import { LINKS, PARAMS } from '../../../../constants/links';
import { useRouteMatch } from 'react-router-dom';
import { BaseFormView } from '../../../layout/BaseFormView';
import { LanguageActions } from '../../../../store/languages/LanguageActions';
import { Button } from '@material-ui/core';
import { confirmation } from '../../../../hooks/confirmation';
import { Validation } from '../../../../constants/GlobalValidationSchema';
import { useRedirect } from '../../../../hooks/useRedirect';
import { T } from '@tolgee/react';
import { ConfirmationDialogProps } from '../../../common/ConfirmationDialog';
import { LanguageModifyFields } from '../../../languages/LanguageModifyFields';
import { components } from '../../../../service/apiSchema.generated';
import { MessageService } from '../../../../service/MessageService';

const actions = container.resolve(LanguageActions);
const messageService = container.resolve(MessageService);

export const LanguageEditView = () => {
  const confirmationMessage = (options: ConfirmationDialogProps) =>
    confirmation({ title: 'Delete language', ...options });

  const match = useRouteMatch();

  const projectId = match.params[PARAMS.PROJECT_ID];
  const languageId = match.params[PARAMS.LANGUAGE_ID] as number;

  const languageLoadable = actions.useSelector((s) => s.loadables.language);
  const editLoadable = actions.useSelector((s) => s.loadables.edit);
  const deleteLoadable = actions.useSelector((s) => s.loadables.delete);

  useEffect(() => {
    if (!languageLoadable.loaded && !languageLoadable.loading) {
      actions.loadableActions.language.dispatch({
        path: {
          projectId: projectId,
          languageId: languageId,
        },
      });
    }
    return () => {
      actions.loadableReset.edit.dispatch();
      actions.loadableReset.language.dispatch();
    };
  }, []);

  useEffect(() => {
    if (deleteLoadable.loaded || editLoadable.loaded) {
      useRedirect(LINKS.PROJECT_EDIT, {
        [PARAMS.PROJECT_ID]: projectId,
      });
    }
    return () => {
      actions.loadableReset.delete.dispatch();
      actions.loadableReset.edit.dispatch();
    };
  }, [deleteLoadable.loaded || editLoadable.loaded]);

  const onSubmit = (values: components['schemas']['LanguageModel']) => {
    const { name, originalName, flagEmoji, tag } = values;
    actions.loadableActions.edit.dispatch({
      path: {
        projectId: projectId,
        languageId: languageId,
      },
      content: {
        'application/json': {
          name,
          originalName,
          tag,
          flagEmoji,
        } as components['schemas']['LanguageDto'],
      },
    });
  };

  return (
    <BaseFormView
      lg={6}
      md={8}
      xs={10}
      title={<T>language_settings_title</T>}
      initialValues={languageLoadable.data!}
      onSubmit={onSubmit}
      saveActionLoadable={editLoadable}
      resourceLoadable={languageLoadable}
      validationSchema={Validation.LANGUAGE}
      customActions={
        <Button
          variant="outlined"
          color="secondary"
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
              onConfirm: () => {
                actions.loadableActions.delete.dispatch({
                  path: {
                    projectId: projectId,
                    languageId: languageId,
                  },
                });
              },
            });
          }}
        >
          <T>delete_language_button</T>
        </Button>
      }
    >
      {() => <LanguageModifyFields />}
    </BaseFormView>
  );
};
