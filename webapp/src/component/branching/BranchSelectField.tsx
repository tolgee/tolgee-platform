import { BranchSelect } from 'tg.component/branching/BranchSelect';
import { useField, useFormikContext } from 'formik';

export const BranchSelectField = ({
  name,
  hideDefault = false,
  hiddenIds,
}: {
  name: string;
  hideDefault?: boolean;
  hiddenIds?: number[];
}) => {
  const { setFieldValue } = useFormikContext<any>();
  const [field] = useField<number | null>(name);

  return (
    <BranchSelect
      branch={field.value || undefined}
      onDefaultValue={(branch) => setFieldValue(name, branch.id)}
      onSelect={(branch) => setFieldValue(name, branch.id)}
      hideDefault={hideDefault}
      hiddenIds={hiddenIds}
    />
  );
};
