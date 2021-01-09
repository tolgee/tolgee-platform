import {default as React, FunctionComponent} from 'react';
import {createStyles, makeStyles} from '@material-ui/core/styles';
import {Checkbox, FormControl, FormControlLabel, FormControlProps, FormGroup, FormHelperText, FormLabel, Theme} from '@material-ui/core';
import {useField} from 'formik';


interface CheckBoxGroupMultiSelectProps {
    name: string;
    label?: string;
    color?: 'primary' | 'secondary' | 'default';
    mt?: number;
    mb?: number;
    options: Set<string>,
}

type Props = CheckBoxGroupMultiSelectProps & FormControlProps


export const CheckBoxGroupMultiSelect: FunctionComponent<Props> = (props) => {

    const useStyles = makeStyles((theme: Theme) =>
        createStyles({
            root: {
                marginTop: theme.spacing(props.mt !== undefined ? props.mt : 2),
                marginBottom: theme.spacing(props.mb !== undefined ? props.mb : 2),
            },
        }),
    );

    const classes = useStyles({});

    const [field, meta, helpers] = useField<Set<string>>(props.name);

    const onChange = (option, checked) => {
        const newValue = new Set(field.value);
        newValue.add(option);
        if (!checked) {
            newValue.delete(option);
        }
        helpers.setValue(newValue);
    };

    return (
        <FormGroup>
            <FormLabel error={!!meta.error} component="legend">{props.label}</FormLabel>
            {Array.from(props.options).map(option =>
                <FormControl className={classes.root} error={!!meta.error}>
                    <FormControlLabel
                        label={option}
                        control={
                            <Checkbox onChange={(e) => onChange(option, e.target.checked)} checked={field.value.has(option)}/>
                        }
                    />
                </FormControl>
            )}
            {!!meta.error &&
            <FormHelperText error={!!meta.error}>{meta.error}</FormHelperText>}
        </FormGroup>
    );
};
