import { FunctionComponent } from 'react';
import { Checkbox as TolgeeCheckbox } from 'tg.component/common/Checkbox';
import { useField } from 'formik';

interface PGCheckboxProps {
  name: string;
}

type Props = PGCheckboxProps & React.ComponentProps<typeof TolgeeCheckbox>;

export const Checkbox: FunctionComponent<Props> = (props) => {
  const [field] = useField(props.name);

  return <TolgeeCheckbox {...field} {...props} />;
};
