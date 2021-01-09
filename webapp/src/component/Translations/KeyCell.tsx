import * as React from 'react';
import {FunctionComponent, useContext} from 'react';
import {RowContext} from "./TranslationsRow";
import {useRepository} from "../../hooks/useRepository";
import {RepositoryPermissionType} from "../../service/response.types";
import {EditableCell} from "./EditableCell";
import {container} from "tsyringe";
import {TranslationActions} from "../../store/repository/TranslationActions";
import {Validation} from "../../constants/GlobalValidationSchema";

let actions = container.resolve(TranslationActions);

export const KeyCell: FunctionComponent = (props) => {
    let repositoryDTO = useRepository();

    let context = useContext(RowContext);

    const handleSubmit = (v) => {
        actions.loadableActions.editKey.dispatch(repositoryDTO.id, {oldFullPathString: context.data.name, newFullPathString: v});
    };

    const isEditing = actions.useSelector(s => s.editing?.type === "key" && s.editing?.data?.initialValue === context.data.name)

    return (
        <EditableCell initialValue={context.data.name}
                      validationSchema={Validation.TRANSLATION_KEY}
                      onSubmit={handleSubmit}
                      editEnabled={repositoryDTO.permissionType === RepositoryPermissionType.MANAGE
                      || repositoryDTO.permissionType === RepositoryPermissionType.EDIT}
                      onChange={(value) => actions.setEditingValue.dispatch(value)}
                      onEditClick={() => {
                          actions.setKeyEditing.dispatch({
                              initialValue: context.data.name,
                              newValue: context.data.name,
                          })
                      }}
                      isEditing={isEditing}
                      onCancel={() => actions.setTranslationEditing.dispatch(null)}
        />
    )
};