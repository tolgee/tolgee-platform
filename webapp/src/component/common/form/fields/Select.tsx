import { FunctionComponent, ReactNode, ComponentProps } from 'react';
import { Select as TolgeeSelect } from 'tg.component/common/Select';
import { useField } from 'formik';

interface PGSelectProps {
  name: string;
  label?: ReactNode;
  renderValue?: (v: any) => ReactNode;
  displayEmpty?: boolean;
}

type Props = PGSelectProps & Partial<ComponentProps<typeof TolgeeSelect>>;

export const Select: FunctionComponent<Props> = (props) => {
  const [field, meta] = useField(props.name);

  const { renderValue, displayEmpty, label, ...formControlProps } = props;

  return (
    <TolgeeSelect
      data-cy="global-form-select"
      labelId={'select_' + field.name + '_label'}
      label={label}
      error={meta.touched ? meta.error : undefined}
      displayEmpty={displayEmpty}
      {...field}
      renderValue={
        typeof renderValue === 'function'
          ? renderValue
          : (value) => value as ReactNode
      }
      {...formControlProps}
    >
      {props.children}
    </TolgeeSelect>
  );
};
