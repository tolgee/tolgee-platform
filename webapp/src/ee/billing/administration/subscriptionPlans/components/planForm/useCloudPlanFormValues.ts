import { useFormikContext } from 'formik';
import { CloudPlanFormData } from './CloudPlanFormBase';

export const useCloudPlanFormValues = (parentName?: string) => {
  const { values, setFieldValue } = useFormikContext<any>();

  return {
    values: (parentName ? values[parentName] : values) as CloudPlanFormData,
    setFieldValue: (name: string, value: any) =>
      setFieldValue(`${parentName}${name}`, value),
  };
};
