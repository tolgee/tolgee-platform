import * as React from 'react';
import { FunctionComponent, useContext } from 'react';
import { RowContext } from './TranslationsRow';
import { useProject } from '../../hooks/useProject';
import { ProjectPermissionType } from '../../service/response.types';
import { EditableCell } from './EditableCell';
import { container } from 'tsyringe';
import { TranslationActions } from '../../store/project/TranslationActions';
import { Validation } from '../../constants/GlobalValidationSchema';

const actions = container.resolve(TranslationActions);

export const KeyCell: FunctionComponent = (props) => {
  const project = useProject();

  const context = useContext(RowContext);

  const handleSubmit = (v) => {
    actions.loadableActions.editKey.dispatch(project.id, {
      oldFullPathString: context.data.name,
      newFullPathString: v,
    });
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
          initialValue: context.data.name,
          newValue: context.data.name,
        });
      }}
      isEditing={isEditing}
      onCancel={() => actions.setTranslationEditing.dispatch(null)}
    />
  );
};
