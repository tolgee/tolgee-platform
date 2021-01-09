import * as React from 'react';
import {ReactNode, useEffect, useState} from 'react';
import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';
import {PropTypes, TextField} from "@material-ui/core";
import Box from "@material-ui/core/Box";
import {T} from '@polygloat/react';

export class ConfirmationDialogProps {
    open?: boolean = true;
    message?: ReactNode = <T>Are you sure?</T>;
    confirmButtonText?: ReactNode = <T>Confirm</T>;
    cancelButtonText?: ReactNode = <T>Cancel</T>;
    title?: ReactNode = <T>Confirmation</T>;
    hardModeText?: string = null;
    confirmButtonColor?: PropTypes.Color = "primary";
    cancelButtonColor?: PropTypes.Color = "default";

    onCancel?: () => void = () => {
    };

    onConfirm?: () => void = () => {
    };
}

export default function ConfirmationDialog(props: ConfirmationDialogProps) {
    props = {...(new ConfirmationDialogProps()), ...props};

    const [input, setInput] = useState("");

    useEffect(() => {
        setInput("");
    }, [props.hardModeText]);

    const disabled = props.hardModeText && props.hardModeText !== input;

    return open ? (

        <Dialog
            open={props.open}
            onClose={props.onCancel}
            aria-labelledby="alert-dialog-title"
            aria-describedby="alert-dialog-description"
        >
            <DialogTitle id="alert-dialog-title">{props.title}</DialogTitle>
            <form onSubmit={(e) => {
                if (!disabled) {
                    props.onConfirm();
                }
                e.preventDefault();
            }}>
                <DialogContent>
                    <DialogContentText id="alert-dialog-description">
                        {props.message}
                    </DialogContentText>
                    {props.hardModeText &&
                    <Box>
                        <TextField fullWidth={true} label={<T parameters={{text: props.hardModeText}}>hard_mode_confirmation_rewrite_text</T>}
                                   value={input}
                                   onChange={(e) => setInput(e.target.value)}/>
                    </Box>
                    }
                </DialogContent>
                <DialogActions>
                    <Button onClick={props.onCancel} type="button" color={props.cancelButtonColor}>
                        {props.cancelButtonText}
                    </Button>
                    <Button
                        color={props.confirmButtonColor}
                        autoFocus
                        disabled={disabled}
                        type="submit">
                        {props.confirmButtonText}
                    </Button>
                </DialogActions>
            </form>
        </Dialog>
    ) : null;
}
