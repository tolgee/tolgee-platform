import { default as React, FunctionComponent, ReactNode } from 'react';
import { T } from '@tolgee/react';

import { TextField } from '../common/form/fields/TextField';

interface SetPasswordFieldsProps {
  label: ReactNode;
}

export const PasswordLabel = () => {
  return <T keyName="Password" />;
};

export const NewPasswordLabel = () => {
  return <T keyName="new-password-input-label" />;
};

export const SetPasswordField: FunctionComponent<SetPasswordFieldsProps> = (
  props
) => {
  return (
    <>
      <TextField
        name="password"
        type="password"
        label={props.label}
        variant="standard"
      />
    </>
  );
};
