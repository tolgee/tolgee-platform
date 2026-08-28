import { FunctionComponent, ReactNode } from 'react';
import { FormikProps } from 'formik';
import { ObjectSchema } from 'yup';

import { LoadableType, StandardForm } from '../common/form/StandardForm';
import { BaseView, BaseViewProps } from './BaseView';

type BaseFormViewProps = {
  initialValues: Record<string, unknown>;
  onSubmit: (v: any) => void;
  onCancel?: () => void;
  validationSchema: ObjectSchema<any>;
  saveActionLoadable?: LoadableType;
  customActions?: ReactNode;
  submitButtonInner?: ReactNode;
  children?: ReactNode | ((formikProps: FormikProps<any>) => ReactNode);
} & (
  | { submitDisabledReason?: ReactNode; disabled?: never }
  | { submitDisabledReason?: never; disabled?: boolean }
);

export const BaseFormView: FunctionComponent<
  BaseFormViewProps & Omit<BaseViewProps, 'children'>
> = (props) => {
  const submitGate =
    props.disabled === undefined
      ? { submitDisabledReason: props.submitDisabledReason }
      : { disabled: props.disabled };

  return (
    <BaseView {...props}>
      <StandardForm
        initialValues={props.initialValues}
        onSubmit={props.onSubmit}
        onCancel={props.onCancel}
        validationSchema={props.validationSchema}
        customActions={props.customActions}
        submitButtonInner={props.submitButtonInner}
        saveActionLoadable={props.saveActionLoadable}
        {...submitGate}
      >
        {props.children}
      </StandardForm>
    </BaseView>
  );
};
