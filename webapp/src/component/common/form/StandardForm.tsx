import {default as React, FunctionComponent, ReactNode} from 'react';
import {Form, Formik, FormikBag, FormikProps} from 'formik';
import {Box, Button} from '@material-ui/core';
import CircularProgress from '@material-ui/core/CircularProgress';
import {ObjectSchema} from 'yup';
import {useHistory} from 'react-router-dom';
import {T} from "@polygloat/react";

interface FormProps<T = { [key: string]: any }> {
    initialValues: T;
    onSubmit: (values: T, formikBag: FormikBag<any, any>) => void | Promise<any>;
    onCancel?: () => void;
    loading?: boolean;
    validationSchema?: ObjectSchema;
    submitButtons?: ReactNode
    customActions?: ReactNode
    submitButtonInner?: ReactNode
}

export const StandardForm: FunctionComponent<FormProps> = ({initialValues, validationSchema, ...props}) => {

    let history = useHistory();

    const onCancel = () => typeof props.onCancel === "function" ? props.onCancel() : history.goBack();

    return (
        <Formik initialValues={initialValues} onSubmit={props.onSubmit} validationSchema={validationSchema} enableReinitialize>
            {(formikProps: FormikProps<any>) => (
                <Form>
                    {typeof props.children === "function" && (!props.loading && props.children(formikProps)) || props.children}
                    {props.loading && <CircularProgress size="small"/>}
                    {(props.submitButtons || (
                        <Box mt={2} display="flex" justifyContent="flex-end">
                            <React.Fragment>
                                {props.customActions && <Box flexGrow={1}>{props.customActions}</Box>}
                                <Box display="flex" alignItems="flex-end" mb={2}>
                                    <Button disabled={props.loading}
                                            onClick={onCancel}><T>global_form_cancel</T></Button>
                                    <Box ml={1}>
                                        <Button color="primary" variant="contained" disabled={props.loading || !formikProps.isValid} type="submit">
                                            {props.submitButtonInner || <T>global_form_save</T>}
                                        </Button>
                                    </Box>
                                </Box>
                            </React.Fragment>
                        </Box>))}
                </Form>
            )}
        </Formik>
    );
};
