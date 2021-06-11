import { useQueryClient } from 'react-query';
import { FunctionComponent, useContext } from 'react';
import { RowContext } from './TranslationsRow';
import { useProject } from '../../hooks/useProject';
import { ProjectPermissionType } from '../../service/response.types';
import { EditableCell } from './EditableCell';
import { container } from 'tsyringe';
import { TranslationActions } from '../../store/project/TranslationActions';
import { useEditKey } from '../../service/hooks/Translation';
import { T } from '@tolgee/react';
import { MessageService } from '../../service/MessageService';
import { Validation } from '../../constants/GlobalValidationSchema';
import { parseErrorResponse } from '../../fixtures/errorFIxtures';

const actions = container.resolve(TranslationActions);
const messaging = container.resolve(MessageService);

export const KeyCell: FunctionComponent = (props) => {
  const project = useProject();
  const queryClient = useQueryClient();

  const context = useContext(RowContext);
  const editKey = useEditKey(project.id);

  const handleSubmit = (v) => {
    editKey.mutate(
      {
        oldFullPathString: context.data.name as string,
        newFullPathString: v,
      },
      {
        onError: (err) => {
          for (const error of parseErrorResponse(err)) {
            messaging.error(<T>{error}</T>);
          }
        },
        onSuccess: () => {
          messaging.success(<T>Translation grid - translation saved</T>);
          queryClient.invalidateQueries([
            'project',
            project.id,
            'translations',
          ]);
          actions.closeKeyEditing.dispatch(true);
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
      onCancel={() => actions.closeKeyEditing.dispatch(false)}
    />
  );
};
