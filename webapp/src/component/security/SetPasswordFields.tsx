import { default as React, FunctionComponent } from 'react';
import { T } from '@tolgee/react';

import { TextField } from '../common/form/fields/TextField';

interface SetPasswordFieldsProps {}

export const SetPasswordFields: FunctionComponent<SetPasswordFieldsProps> = (
  props
) => {
  return (
    <>
      <TextField
        name="password"
        type="password"
        label={<T keyName="Password" />}
        variant="standard"
      />
      <TextField
        name="passwordRepeat"
        type="password"
        label={<T keyName="Password confirmation" />}
        variant="standard"
      />
    </>
  );
};
