import {
  default as React,
  FunctionComponent,
  ReactElement,
  ReactNode,
} from 'react';
import { FieldArray as FormikFieldArray, useField } from 'formik';

export interface FAProps<T extends FunctionComponent> {
  name: string;
  children: (
    nameCallback: (fieldName: string) => string,
    index: number,
    removeItem: () => void
  ) => ReactElement;
  defaultItemValue?: any;
  addButton: (addItem: () => void) => ReactNode;
  showArrayErrors?: boolean;
}

export const FieldArray = <T extends FunctionComponent>(props: FAProps<T>) => {
  const [field, _] = useField(props.name);
  const values = field.value;

  console.log(values);

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
                  console.log('remove');
                  arrayHelpers.remove(index);
                }
              )
            )}
            {props.addButton(() => {
              console.log('insert');
              arrayHelpers.insert(values.length, props.defaultItemValue);
            })}
          </>
        )}
      />
    </>
  );
};
