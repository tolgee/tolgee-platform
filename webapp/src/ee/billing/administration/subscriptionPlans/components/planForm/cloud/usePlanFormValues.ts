import { useFormikContext } from 'formik';
import { GenericPlanFormData } from './types';

export const usePlanFormValues = <
  T extends GenericPlanFormData = GenericPlanFormData
>(
  parentName?: string
) => {
  const { values, setFieldValue } = useFormikContext<any>();

  // parent name can be empty string or can end with a dot
  // for purpose of getting values, we need to remove the dot
  if (parentName && parentName.endsWith('.')) {
    parentName = parentName.slice(0, -1);
  }

  return {
    values: (parentName ? values[parentName] : values) as T,
    setFieldValue: (name: string, value: any) =>
      setFieldValue(`${parentName}${name}`, value),
  };
};
