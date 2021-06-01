import { default as React, FunctionComponent, ReactNode } from 'react';
import { Form, Formik, FormikProps } from 'formik';
import { Box, Button } from '@material-ui/core';
import CircularProgress from '@material-ui/core/CircularProgress';
import { ObjectSchema } from 'yup';
import { useHistory } from 'react-router-dom';
import { T } from '@tolgee/react';
import { ResourceErrorComponent } from './ResourceErrorComponent';
import { Loadable } from '../../../store/AbstractLoadableActions';
import LoadingButton from './LoadingButton';
import { FormikHelpers } from 'formik/dist/types';

interface FormProps<T> {
  initialValues: T;
  onSubmit: (values: T, formikHelpers: FormikHelpers<T>) => void | Promise<any>;
  onCancel?: () => void;
  loading?: boolean;
  validationSchema?: ObjectSchema<any>;
  submitButtons?: ReactNode;
  customActions?: ReactNode;
  submitButtonInner?: ReactNode;
  saveActionLoadable?: Loadable;
}

export const StandardForm: FunctionComponent<FormProps<any>> = ({
  initialValues,
  validationSchema,
  ...props
}) => {
  let history = useHistory();

  const onCancel = () =>
    typeof props.onCancel === 'function' ? props.onCancel() : history.goBack();

  return (
    <>
      {props.saveActionLoadable && props.saveActionLoadable.error && (
        <ResourceErrorComponent error={props.saveActionLoadable.error} />
      )}

      <Formik
        initialValues={initialValues}
        onSubmit={props.onSubmit}
        validationSchema={validationSchema}
        enableReinitialize
      >
        {(formikProps: FormikProps<any>) => {
          return (
            <Form>
              {(typeof props.children === 'function' &&
                !props.loading &&
                props.children(formikProps)) ||
                props.children}
              {props.loading && <CircularProgress size="small" />}
              {props.submitButtons || (
                <Box display="flex" justifyContent="flex-end">
                  <React.Fragment>
                    {props.customActions && (
                      <Box flexGrow={1}>{props.customActions}</Box>
                    )}
                    <Box display="flex" alignItems="flex-end" mb={2}>
                      <Button
                        data-cy="global-form-cancel-button"
                        disabled={props.loading}
                        onClick={onCancel}
                      >
                        <T>global_form_cancel</T>
                      </Button>
                      <Box ml={1}>
                        <LoadingButton
                          data-cy="global-form-save-button"
                          loading={props.saveActionLoadable?.loading}
                          color="primary"
                          variant="contained"
                          disabled={props.loading}
                          type="submit"
                        >
                          {props.submitButtonInner || <T>global_form_save</T>}
                        </LoadingButton>
                      </Box>
                    </Box>
                  </React.Fragment>
                </Box>
              )}
            </Form>
          );
        }}
      </Formik>
    </>
  );
};
