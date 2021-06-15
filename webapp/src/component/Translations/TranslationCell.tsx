import { useQueryClient } from 'react-query';
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
import { T } from '@tolgee/react';
import { MessageService } from '../../service/MessageService';
import { Validation } from '../../constants/GlobalValidationSchema';
import { useSetTranslations } from '../../service/hooks/Translation';
import { parseErrorResponse } from '../../fixtures/errorFIxtures';

export interface TranslationsTableCellProps {
  abbreviation: string;
}

const actions = container.resolve(TranslationActions);
const messaging = container.resolve(MessageService);

export const TranslationCell: FunctionComponent<TranslationsTableCellProps> = (
  props
) => {
  const queryClient = useQueryClient();

  const projectDTO = useProject();
  const context = useContext(RowContext);

  const setTranslations = useSetTranslations(projectDTO.id);

  const handleSubmit = (v) => {
    setTranslations.mutate(
      {
        key: context.data.name as string,
        translations: { [props.abbreviation]: v },
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
            projectDTO.id,
            'translations',
          ]);
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
          data: {
            initialValue,
            key: context.data.name as string,
            newValue: initialValue,
            languageAbbreviation: props.abbreviation,
          },
        });
      }}
      isEditing={isEditing}
      onCancel={() => actions.setTranslationEditing.dispatch({ data: null })}
      lang={props.abbreviation}
    />
  );
};
