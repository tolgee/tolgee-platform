import { FunctionComponent, useContext } from 'react';
import { RowContext } from './TranslationsRow';
import { useProject } from 'tg.hooks/useProject';
import { ProjectPermissionType } from 'tg.service/response.types';
import { EditableCell } from './EditableCell';
import { container } from 'tsyringe';
import {
  TranslationActions,
  TranslationEditingType,
} from 'tg.store/project/TranslationActions';
import { T } from '@tolgee/react';
import { MessageService } from 'tg.service/MessageService';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { useApiMutation } from 'tg.service/http/useQueryApi';

export interface TranslationsTableCellProps {
  tag: string;
}

const actions = container.resolve(TranslationActions);
const messaging = container.resolve(MessageService);

export const TranslationCell: FunctionComponent<TranslationsTableCellProps> = (
  props
) => {
  const projectDTO = useProject();
  const context = useContext(RowContext);

  const setTranslations = useApiMutation({
    url: '/api/project/{projectId}/translations',
    method: 'put',
    invalidatePrefix: '/api/project',
  });

  const handleSubmit = (v) => {
    setTranslations.mutate(
      {
        path: { projectId: projectDTO.id },
        content: {
          'application/json': {
            key: context.data.name as string,
            translations: { [props.tag]: v },
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
          messaging.success(<T>Translation grid - translation saved</T>);
          actions.setTranslationEditing.dispatch({
            data: null,
            skipConfirm: true,
          });
        },
      }
    );
  };

  const isEditing = actions.useSelector((s) => {
    const data = s.editing?.data as TranslationEditingType;
    return (
      s.editing?.type === 'translation' &&
      data?.languageAbbreviation === props.tag &&
      data?.key === context.data.name
    );
  });

  const initialValue = context.data.translations[props.tag];

  return (
    <EditableCell
      initialValue={context.data.translations[props.tag]}
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
          data: {
            initialValue,
            key: context.data.name as string,
            newValue: initialValue,
            languageAbbreviation: props.tag,
          },
        });
      }}
      isEditing={isEditing}
      onCancel={() => actions.setTranslationEditing.dispatch({ data: null })}
      lang={props.tag}
    />
  );
};
