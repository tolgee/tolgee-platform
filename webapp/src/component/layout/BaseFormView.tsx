import { FunctionComponent, ReactNode } from 'react';
import { FormikProps } from 'formik';
import { ObjectSchema } from 'yup';

import { Link } from 'tg.constants/links';
import { LoadableType, StandardForm } from '../common/form/StandardForm';
import { BaseView, BaseViewProps } from './BaseView';

interface BaseFormViewProps {
  initialValues: Record<string, unknown>;
  onSubmit: (v: any) => void;
  onCancel?: () => void;
  validationSchema: ObjectSchema<any>;
  saveActionLoadable?: LoadableType;
  redirectAfter?: Link;
  customActions?: ReactNode;
  submitButtonInner?: ReactNode;
  submitDisabledReason?: ReactNode;
  disabled?: boolean;
  children?: ReactNode | ((formikProps: FormikProps<any>) => ReactNode);
}

export const BaseFormView: FunctionComponent<
  BaseFormViewProps & Omit<BaseViewProps, 'children'>
> = (props) => {
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
        submitDisabledReason={props.submitDisabledReason}
        disabled={props.disabled}
      >
        {props.children}
      </StandardForm>
    </BaseView>
  );
};
