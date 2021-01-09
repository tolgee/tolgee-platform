import * as React from 'react';
import {FunctionComponent, useContext} from 'react';
import {RowContext} from "./TranslationsRow";
import {useRepository} from "../../hooks/useRepository";
import {RepositoryPermissionType} from "../../service/response.types";
import {EditableCell} from "./EditableCell";
import {container} from "tsyringe";
import {TranslationActions, TranslationEditingType} from "../../store/repository/TranslationActions";
import {Validation} from "../../constants/GlobalValidationSchema";

export interface TranslationsTableCellProps {
    abbreviation: string;
}

let actions = container.resolve(TranslationActions);

export const TranslationCell: FunctionComponent<TranslationsTableCellProps> = (props) => {
    let repositoryDTO = useRepository();
    let context = useContext(RowContext);

    const handleSubmit = (v) => {
        actions.loadableActions.setTranslations.dispatch(repositoryDTO.id, {key: context.data.name, translations: {[props.abbreviation]: v}});
    };

    const isEditing = actions.useSelector(s => {
        const data = s.editing?.data as TranslationEditingType;
        return s.editing?.type === "translation" && data?.languageAbbreviation === props.abbreviation && data?.key === context.data.name;
    })

    const initialValue = context.data.translations[props.abbreviation];

    return (
        <EditableCell initialValue={context.data.translations[props.abbreviation]}
                      validationSchema={Validation.TRANSLATION_TRANSLATION}
                      onSubmit={handleSubmit}
                      editEnabled={repositoryDTO.permissionType === RepositoryPermissionType.MANAGE
                      || repositoryDTO.permissionType === RepositoryPermissionType.EDIT || repositoryDTO.permissionType === RepositoryPermissionType.TRANSLATE}
                      onChange={(value) => actions.setEditingValue.dispatch(value)}
                      onEditClick={() => {
                          actions.setTranslationEditing.dispatch({
                              initialValue,
                              key: context.data.name,
                              newValue: initialValue,
                              languageAbbreviation: props.abbreviation
                          })
                      }}
                      isEditing={isEditing}
                      onCancel={() => actions.setTranslationEditing.dispatch(null)}
        />
    )
};