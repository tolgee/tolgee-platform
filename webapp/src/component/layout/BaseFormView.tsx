import {default as React, FunctionComponent, ReactNode} from 'react';
import {BaseView, BaseViewProps} from './BaseView';
import {StandardForm} from '../common/form/StandardForm';
import {ObjectSchema} from 'yup';
import {ResourceErrorComponent} from "../common/form/ResourceErrorComponent";
import {Link} from "../../constants/links";
import {Loadable} from "../../store/AbstractLoadableActions";

interface BaseFormViewProps {
    onInit?: () => {};
    saving?: boolean;
    initialValues: object;
    onSubmit: (v: object) => void,
    onCancel?: () => void,
    validationSchema: ObjectSchema,
    resourceLoadable?: Loadable<any>,
    saveActionLoadable?: Loadable<any>,
    redirectAfter?: Link;
    customActions?: ReactNode;
    submitButtons?: ReactNode;
    submitButtonInner?: ReactNode;
}

export const BaseFormView: FunctionComponent<BaseFormViewProps & BaseViewProps> = (props) => {

    return (
        <BaseView loading={props.resourceLoadable && !props.resourceLoadable.data || (props.resourceLoadable && !props.resourceLoadable.touched)} {...props}>
            {props.saveActionLoadable && props.saveActionLoadable.error && <ResourceErrorComponent error={props.saveActionLoadable.error}/>}

            <StandardForm initialValues={props.initialValues} onSubmit={props.onSubmit}
                          onCancel={props.onCancel}
                          validationSchema={props.validationSchema}
                          customActions={props.customActions}
                          submitButtons={props.submitButtons}
                          submitButtonInner={props.submitButtonInner}
            >
                {props.children}
            </StandardForm>
        </BaseView>
    );
};
