import {default as React, FunctionComponent} from 'react';
import {createStyles, makeStyles} from '@material-ui/core/styles';
import {TextField as MUITextField, TextFieldProps, Theme} from '@material-ui/core';
import {useField} from 'formik';

interface PGTextFieldProps {
    name: string;
}

type Props = PGTextFieldProps & TextFieldProps

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        textField: {
            marginTop: theme.spacing(2),
            marginBottom: theme.spacing(2),
        },
    }),
);


export const TextField: FunctionComponent<Props> = (props) => {
    const classes = useStyles({});

    const [field, meta, helpers] = useField(props.name);
    return <MUITextField className={props.className || classes.textField} fullWidth={props.fullWidth ? props.fullWidth : true}
                         {...field} {...props} helperText={meta.error} error={!!meta.error}/>;
};
