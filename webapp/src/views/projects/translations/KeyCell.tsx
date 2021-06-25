import { FunctionComponent, useContext } from 'react';
import { T } from '@tolgee/react';
import { container } from 'tsyringe';

import { Validation } from 'tg.constants/GlobalValidationSchema';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { useProject } from 'tg.hooks/useProject';
import { MessageService } from 'tg.service/MessageService';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { ProjectPermissionType } from 'tg.service/response.types';
import { TranslationActions } from 'tg.store/project/TranslationActions';

import { EditableCell } from './EditableCell';
import { RowContext } from './TranslationsRow';

const actions = container.resolve(TranslationActions);
const messaging = container.resolve(MessageService);

export const KeyCell: FunctionComponent = (props) => {
  const project = useProject();

  const context = useContext(RowContext);
  const editKey = useApiMutation({
    url: '/api/project/{projectId}/keys/edit',
    method: 'post',
    invalidatePrefix: '/api/project',
  });

  const handleSubmit = (v) => {
    editKey.mutate(
      {
        path: { projectId: project.id },
        content: {
          'application/json': {
            oldFullPathString: context.data.name as string,
            newFullPathString: v,
          },
        },
      },
      {
        onError: (err) => {
          for (const error of parseErrorResponse(err)) {
            messaging.error(<T>{error}</T>);
          }
        },
        onSuccess: () => {
          messaging.success(<T>Translation grid - Successfully edited!</T>);
          actions.setTranslationEditing.dispatch({
            data: null,
            skipConfirm: true,
          });
        },
      }
    );
  };

  const isEditing = actions.useSelector(
    (s) =>
      s.editing?.type === 'key' &&
      s.editing?.data?.initialValue === context.data.name
  );

  return (
    <EditableCell
      initialValue={context.data.name}
      validationSchema={Validation.TRANSLATION_KEY}
      onSubmit={handleSubmit}
      editEnabled={
        project.computedPermissions === ProjectPermissionType.MANAGE ||
        project.computedPermissions === ProjectPermissionType.EDIT
      }
      onChange={(value) => actions.setEditingValue.dispatch(value)}
      onEditClick={() => {
        actions.setKeyEditing.dispatch({
          initialValue: context.data.name as string,
          newValue: context.data.name as string,
        });
      }}
      isEditing={isEditing}
      onCancel={() =>
        actions.setTranslationEditing.dispatch({
          data: null,
        })
      }
    />
  );
};
