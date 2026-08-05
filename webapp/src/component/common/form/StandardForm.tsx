import { default as React, ReactNode } from 'react';
import { Box, Button, SxProps } from '@mui/material';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { T } from '@tolgee/react';
import { Form, Formik, FormikProps } from 'formik';
import { FormikHelpers, FormikValues } from 'formik/dist/types';
import { useHistory } from 'react-router-dom';
import { ObjectSchema } from 'yup';

import LoadingButton from './LoadingButton';
import { ResourceErrorComponent } from './ResourceErrorComponent';
import { ApiError } from 'tg.service/http/ApiError';

export interface LoadableType {
  loading?: boolean;
  isLoading?: boolean;
  error?: ApiError | null;
}

interface FormPropsBase<T> {
  initialValues: T;
  onSubmit: (values: T, formikHelpers: FormikHelpers<T>) => void | Promise<any>;
  onCancel?: (formikHelpers: FormikProps<T>) => void;
  loading?: boolean;
  validationSchema?: ObjectSchema<any>;
  saveActionLoadable?: LoadableType;
  children: ReactNode | ((formikProps: FormikProps<T>) => ReactNode);
  rootSx?: SxProps;
  showResourceError?: boolean;
  formId?: string;
}

type FooterProps =
  | {
      submitButtons?: undefined;
      submitDisabledReason?: ReactNode;
      submitButtonInner?: ReactNode;
      customActions?: ReactNode;
      hideCancel?: boolean;
      disabled?: boolean;
    }
  | {
      submitButtons: ReactNode;
      submitDisabledReason?: never;
      submitButtonInner?: never;
      customActions?: never;
      hideCancel?: never;
      disabled?: never;
    };

type FormProps<T> = FormPropsBase<T> & FooterProps;

export function StandardForm<T extends FormikValues>({
  initialValues,
  validationSchema,
  disabled,
  submitDisabledReason,
  rootSx = { mb: 2 },
  hideCancel,
  showResourceError = true,
  formId,
  ...props
}: FormProps<T>) {
  const history = useHistory();

  const actionLoading =
    props.saveActionLoadable?.isLoading || props.saveActionLoadable?.loading;

  const submitBlocked = Boolean(
    props.loading || disabled || submitDisabledReason
  );

  return (
    <>
      {showResourceError &&
        props.saveActionLoadable &&
        props.saveActionLoadable.error && (
          <ResourceErrorComponent error={props.saveActionLoadable.error} />
        )}

      <Formik
        initialValues={initialValues}
        onSubmit={(values, helpers) => {
          if (submitBlocked) {
            helpers.setSubmitting(false);
            return;
          }
          return props.onSubmit(values, helpers);
        }}
        validationSchema={validationSchema}
        enableReinitialize
      >
        {(formikProps: FormikProps<any>) => {
          const onCancel = () =>
            typeof props.onCancel === 'function'
              ? props.onCancel(formikProps)
              : history.goBack();

          return (
            <Form id={formId}>
              {typeof props.children === 'function'
                ? !props.loading && props.children(formikProps)
                : props.children}
              {submitDisabledReason && (
                <Box display="flex" justifyContent="flex-end" sx={{ mb: 1 }}>
                  {submitDisabledReason}
                </Box>
              )}
              {props.submitButtons || (
                <Box display="flex" justifyContent="flex-end" sx={rootSx}>
                  <React.Fragment>
                    {props.customActions && (
                      <Box flexGrow={1}>{props.customActions}</Box>
                    )}
                    <Box display="flex" alignItems="flex-end">
                      {!hideCancel && (
                        <Button
                          data-cy="global-form-cancel-button"
                          onClick={onCancel}
                        >
                          <T keyName="global_form_cancel" />
                        </Button>
                      )}
                      <Box ml={1}>
                        <LoadingButton
                          data-cy="global-form-save-button"
                          loading={actionLoading}
                          color="primary"
                          variant="contained"
                          disabled={submitBlocked}
                          type="submit"
                        >
                          {props.submitButtonInner || (
                            <T keyName="global_form_save" />
                          )}
                        </LoadingButton>
                      </Box>
                    </Box>
                  </React.Fragment>
                </Box>
              )}
              {props.loading && (
                <Box justifyContent="cetner">
                  <SpinnerProgress />
                </Box>
              )}
            </Form>
          );
        }}
      </Formik>
    </>
  );
}
