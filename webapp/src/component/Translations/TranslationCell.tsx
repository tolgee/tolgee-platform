import * as React from 'react';
import { FunctionComponent, useContext } from 'react';
import { RowContext } from './TranslationsRow';
import { useProject } from '../../hooks/useProject';
import { ProjectPermissionType } from '../../service/response.types';
import { EditableCell } from './EditableCell';
import { container } from 'tsyringe';
import {
  TranslationActions,
  TranslationEditingType,
} from '../../store/project/TranslationActions';
import { Validation } from '../../constants/GlobalValidationSchema';

export interface TranslationsTableCellProps {
  abbreviation: string;
}

let actions = container.resolve(TranslationActions);

export const TranslationCell: FunctionComponent<TranslationsTableCellProps> = (
  props
) => {
  let projectDTO = useProject();
  let context = useContext(RowContext);

  const handleSubmit = (v) => {
    actions.loadableActions.setTranslations.dispatch(projectDTO.id, {
      key: context.data.name,
      translations: { [props.abbreviation]: v },
    });
  };

  const isEditing = actions.useSelector((s) => {
    const data = s.editing?.data as TranslationEditingType;
    return (
      s.editing?.type === 'translation' &&
      data?.languageAbbreviation === props.abbreviation &&
      data?.key === context.data.name
    );
  });

  const initialValue = context.data.translations[props.abbreviation];

  return (
    <EditableCell
      initialValue={context.data.translations[props.abbreviation]}
      validationSchema={Validation.TRANSLATION_TRANSLATION}
      onSubmit={handleSubmit}
      editEnabled={
        projectDTO.computedPermissions === ProjectPermissionType.MANAGE ||
        projectDTO.computedPermissions === ProjectPermissionType.EDIT ||
        projectDTO.computedPermissions === ProjectPermissionType.TRANSLATE
      }
      onChange={(value) => actions.setEditingValue.dispatch(value)}
      onEditClick={() => {
        actions.setTranslationEditing.dispatch({
          initialValue,
          key: context.data.name,
          newValue: initialValue,
          languageAbbreviation: props.abbreviation,
        });
      }}
      isEditing={isEditing}
      onCancel={() => actions.setTranslationEditing.dispatch(null)}
      lang={props.abbreviation}
    />
  );
};
