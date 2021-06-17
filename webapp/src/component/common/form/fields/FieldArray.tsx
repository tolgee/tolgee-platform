import { default as React, FC, ReactElement, ReactNode } from 'react';
import { FieldArray as FormikFieldArray, useField } from 'formik';

export const FieldArray: FC<{
  name: string;
  children: (
    nameCallback: (fieldName: string) => string,
    index: number,
    removeItem: () => void
  ) => ReactElement;
  defaultItemValue?: any;
  addButton: (addItem: () => void) => ReactNode;
  showArrayErrors?: boolean;
}> = (props) => {
  const [field, _] = useField(props.name);
  const values = field.value;

  return (
    <>
      <FormikFieldArray
        name={props.name}
        render={(arrayHelpers) => (
          <>
            {values.map((value, index) =>
              props.children(
                (name) => `${props.name}.${index}.${name}`,
                index,
                () => {
                  arrayHelpers.remove(index);
                }
              )
            )}
            {props.addButton(() => {
              arrayHelpers.insert(values.length, props.defaultItemValue);
            })}
          </>
        )}
      />
    </>
  );
};
