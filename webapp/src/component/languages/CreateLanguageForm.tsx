import React, {FunctionComponent} from 'react';
import {StandardForm} from "../common/form/StandardForm";
import {Validation} from "../../constants/GlobalValidationSchema";
import {TextField} from "../common/form/fields/TextField";
import {T} from "@tolgee/react";
import {LanguageDTO} from "../../service/response.types";
import {Loadable} from "../../store/AbstractLoadableActions";

export const CreateLanguageForm: FunctionComponent<{
    onCancel: () => void,
    onSubmit: (language: LanguageDTO) => void,
    loadable: Loadable
}> = (props) => {

    return (
        <StandardForm initialValues={{name: "", abbreviation: ""}}
                      onSubmit={props.onSubmit}
                      onCancel={props.onCancel}
                      saveActionLoadable={props.loadable}
                      validationSchema={Validation.LANGUAGE}
        >
            <>
                <TextField label={<T>language_create_edit_language_name_label</T>} name="name" required={true}/>
                <TextField label={<T>language_create_edit_abbreviation</T>} name="abbreviation" required={true}/>
            </>
        </StandardForm>
    );
};
