import {default as React, FunctionComponent, ReactElement} from "react";
import {FieldArray as FormikFieldArray, useField} from "formik";
import {Box} from "@material-ui/core";
import IconButton from "@material-ui/core/IconButton";
import RemoveIcon from "@material-ui/icons/Remove";
import AddIcon from "@material-ui/icons/Add";
import Button from "@material-ui/core/Button";
import FormHelperText from "@material-ui/core/FormHelperText";

export interface FAProps {
    name: string,
    children: (nameCallback: (fieldName: string) => string) => ReactElement;
    initialValue?: any[];
    addText?: string
}

export const FieldArray: FunctionComponent<FAProps> = (props) => {
    const [field, meta, helpers] = useField(props.name);
    const values = field.value;
    return (
        <FormikFieldArray
            name={props.name}
            render={arrayHelpers => (
                <div>
                    {values && values.length > 0 ? (
                        <>
                            {values.map((value, index) => (
                                <Box key={index} display="flex">
                                    <Box flexGrow={1} justifyContent="space-between" display="flex">
                                        {props.children((name) => `${props.name}.${index}.${name}`)}
                                        <Box display="flex" alignItems="center">
                                            <IconButton
                                                type="button"
                                                onClick={() => arrayHelpers.remove(index)} // remove a friend from the list
                                            >
                                                <RemoveIcon/>
                                            </IconButton>
                                        </Box>
                                    </Box>
                                </Box>
                            ))}
                            <Box display="flex" alignItems="center" justifyContent="flex-end">
                                <IconButton
                                    type="button"
                                    onClick={() => arrayHelpers.insert(values.length, props.initialValue)} // insert an empty string at a position
                                >
                                    <AddIcon/>
                                </IconButton>
                            </Box>
                        </>

                    ) : (
                        <>
                            <Box>
                                <Button variant="outlined" onClick={() => arrayHelpers.push('')}>
                                    Add {field.name}
                                </Button>
                            </Box>
                            <FormHelperText error={!!meta.error} color="textSecondary">{meta.error}</FormHelperText>
                        </>
                    )}
                </div>
            )}
        />);
};
