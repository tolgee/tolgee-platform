import { BranchSelect } from 'tg.component/branching/BranchSelect';
import { useField, useFormikContext } from 'formik';

export const BranchSelectField = ({
  name,
  hideDefault = false,
}: {
  name: string;
  hideDefault?: boolean;
}) => {
  const { setFieldValue } = useFormikContext<any>();
  const [field] = useField<number | null>(name);

  return (
    <BranchSelect
      branch={field.value || undefined}
      onDefaultValue={(branch) => setFieldValue(name, branch.id)}
      onSelect={(branch) => setFieldValue(name, branch.id)}
      hideDefault={hideDefault}
    />
  );
};
