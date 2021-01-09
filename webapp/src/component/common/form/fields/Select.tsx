import {default as React, FunctionComponent, ReactNode} from 'react';
import {createStyles, makeStyles} from '@material-ui/core/styles';
import {FormControl, FormControlProps, FormHelperText, InputLabel, Select as MUISelect, Theme} from '@material-ui/core';
import {useField} from 'formik';


interface PGSelectProps {
    name: string;
    label?: ReactNode;
    renderValue?: (v: any) => ReactNode;
}

type Props = PGSelectProps & FormControlProps

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        select: {
            marginTop: theme.spacing(2),
            marginBottom: theme.spacing(2),
            minWidth: 120
        },
    }),
);


export const Select: FunctionComponent<Props> = (props) => {
    const classes = useStyles({});

    const [field, meta, helpers] = useField(props.name);

    const {renderValue, ...formControlProps} = props;

    return (
        <FormControl className={classes.select} error={!!meta.error} {...formControlProps} >
            {props.label && <InputLabel id={'select_' + field.name + '_label'}>{props.label}</InputLabel>}
            <MUISelect
                name={field.name}
                labelId={'select_' + field.name + '_label'}
                value={field.value}
                onChange={e => helpers.setValue(e.target.value)}
                renderValue={typeof renderValue === 'function' ? renderValue : value => value}
            >
                {props.children}
            </MUISelect>
            {meta.error &&
            <FormHelperText>{meta.error}</FormHelperText>}
        </FormControl>
    );
};
