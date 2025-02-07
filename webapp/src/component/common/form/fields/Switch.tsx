import { FunctionComponent, useEffect, useState } from 'react';
import { Switch as TolgeeSwitch } from 'tg.component/common/Switch';
import { useField } from 'formik';

interface PGSwitchProps {
  name: string;
  onValueChange?: (newValue: boolean) => void;
}

type Props = PGSwitchProps & React.ComponentProps<typeof TolgeeSwitch>;

export const Switch: FunctionComponent<Props> = (props) => {
  const [field] = useField(props.name);
  const [oldValue, setOldValue] = useState(field.value);

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { onValueChange, ...otherProps } = props;

  useEffect(() => {
    if (typeof props.onValueChange === 'function' && oldValue !== field.value) {
      props.onValueChange(field.value);
      setOldValue(field.value);
    }
  });

  return <TolgeeSwitch checked={field.value} {...field} {...otherProps} />;
};
