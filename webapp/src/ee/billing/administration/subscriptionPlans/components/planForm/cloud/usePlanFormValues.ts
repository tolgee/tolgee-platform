import { useFormikContext } from 'formik';
import { GenericPlanFormData } from './types';

export const usePlanFormValues = <
  T extends GenericPlanFormData = GenericPlanFormData
>(
  parentName?: string
) => {
  const { values, setFieldValue } = useFormikContext<any>();

  return {
    values: (parentName ? values[parentName] : values) as T,
    setFieldValue: (name: string, value: any) =>
      setFieldValue(`${parentName}${name}`, value),
  };
};
