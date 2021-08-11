import { FunctionComponent, ReactNode } from 'react';
import { ObjectSchema } from 'yup';

import { Link } from 'tg.constants/links';
import { LoadableType, StandardForm } from '../common/form/StandardForm';
import { BaseView, BaseViewProps } from './BaseView';

interface BaseFormViewProps {
  onInit?: () => Record<string, unknown>;
  saving?: boolean;
  initialValues: Record<string, unknown>;
  onSubmit: (v: any) => void;
  onCancel?: () => void;
  validationSchema: ObjectSchema<any>;
  saveActionLoadable?: LoadableType;
  redirectAfter?: Link;
  customActions?: ReactNode;
  submitButtons?: ReactNode;
  submitButtonInner?: ReactNode;
}

export const BaseFormView: FunctionComponent<
  BaseFormViewProps & BaseViewProps
> = (props) => {
  return (
    <BaseView {...props}>
      <StandardForm
        initialValues={props.initialValues}
        onSubmit={props.onSubmit}
        onCancel={props.onCancel}
        validationSchema={props.validationSchema}
        customActions={props.customActions}
        submitButtons={props.submitButtons}
        submitButtonInner={props.submitButtonInner}
        saveActionLoadable={props.saveActionLoadable}
      >
        {props.children}
      </StandardForm>
    </BaseView>
  );
};
