import {default as React, FunctionComponent} from 'react';
import {TextField} from '../common/form/fields/TextField';
import {T} from "@polygloat/react";

interface SetPasswordFieldsProps {

}

export const SetPasswordFields: FunctionComponent<SetPasswordFieldsProps> = (props) => {
    return (
        <>
            <TextField name="password" type="password" label={<T>Password</T>}/>
            <TextField name="passwordRepeat" type="password" label={<T>Password confirmation</T>}/>
        </>
    );
};
