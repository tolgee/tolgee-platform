import { default as React, FunctionComponent, ReactNode } from 'react';
import { Box, Button } from '@material-ui/core';
import CircularProgress from '@material-ui/core/CircularProgress';
import { T } from '@tolgee/react';
import { Form, Formik, FormikProps } from 'formik';
import { FormikHelpers } from 'formik/dist/types';
import { useHistory } from 'react-router-dom';
import { ObjectSchema } from 'yup';

import { ErrorResponseDto } from 'tg.service/response.types';

import LoadingButton from './LoadingButton';
import { ResourceErrorComponent } from './ResourceErrorComponent';

export interface LoadableType {
  loading?: boolean;
  isLoading?: boolean;
  error?: ErrorResponseDto;
}

interface FormProps<T> {
  initialValues: T;
  onSubmit: (values: T, formikHelpers: FormikHelpers<T>) => void | Promise<any>;
  onCancel?: (formikHelpers: FormikProps<T>) => void;
  loading?: boolean;
  validationSchema?: ObjectSchema<any>;
  submitButtons?: ReactNode;
  customActions?: ReactNode;
  submitButtonInner?: ReactNode;
  saveActionLoadable?: LoadableType;
}

export const StandardForm: FunctionComponent<FormProps<any>> = ({
  initialValues,
  validationSchema,
  ...props
}) => {
  const history = useHistory();

  const actionLoading =
    props.saveActionLoadable?.isLoading || props.saveActionLoadable?.loading;

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
          const onCancel = () =>
            typeof props.onCancel === 'function'
              ? props.onCancel(formikProps)
              : history.goBack();

          return (
            <Form>
              {(typeof props.children === 'function' &&
                !props.loading &&
                props.children(formikProps)) ||
                props.children}
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
                          loading={actionLoading}
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
              {props.loading && (
                <Box justifyContent="cetner">
                  <CircularProgress />
                </Box>
              )}
            </Form>
          );
        }}
      </Formik>
    </>
  );
};
