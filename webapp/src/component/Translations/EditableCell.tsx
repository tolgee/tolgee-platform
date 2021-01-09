import * as React from 'react';
import {FunctionComponent, useEffect, useState} from 'react';
import {Box, Theme, Tooltip, Typography} from "@material-ui/core";
import {MicroForm} from "../common/form/MicroForm";
import {EasyInput} from "../common/form/fields/EasyInput";
import InputAdornment from "@material-ui/core/InputAdornment";
import IconButton from "@material-ui/core/IconButton";
import CheckIcon from '@material-ui/icons/Check';
import CloseIcon from '@material-ui/icons/Close';
import * as Yup from 'yup';
import {EditIconButton} from "../common/buttons/EditIconButton";
import EditIcon from "@material-ui/icons/Edit";
import {createStyles, makeStyles} from "@material-ui/core/styles";

export interface EditableCellProps {
    initialValue: any,
    validationSchema: Yup.Schema<any>,
    onSubmit: (value: string) => void;
    onChange?: (value: string) => void;
    editEnabled: boolean;
    isEditing: boolean;
    onCancel?: (value: string) => void
    onEditClick: () => void
}

const useStyles = makeStyles((theme: Theme) => createStyles({
    textBox: {
        maxWidth: "100%",
        '& .hiding-click-icon': {
            opacity: "0.1",
            padding: 0,
            transition: "opacity 0.2s",
            "&.Mui-focusVisible": {
                opacity: 1
            }
        },
        '&:hover .hiding-click-icon': {
            opacity: 0.5
        }
    },
}));

export const EditableCell: FunctionComponent<EditableCellProps> = (props) => {
    const [overflow, setOverflow] = useState(false);
    const [value, setValue] = useState(props.initialValue)

    const onInputChange = (e) => {
        setValue(e.target.value);
        props.onChange(e.target.value);
    }

    let ref = React.createRef<HTMLDivElement>();

    useEffect(() => {
        const onresize = () => {
            if (ref.current) {
                setOverflow(ref.current.scrollWidth > ref.current.clientWidth);
            }
        };

        onresize();

        window.addEventListener("resize", onresize);

        return () => window.removeEventListener('resize', onresize);

    }, [ref]);


    const classes = useStyles({});

    const textHolder = <Typography noWrap ref={ref} style={{fontSize: "inherit"}}>
        {props.initialValue}
    </Typography>;

    const EditIconButton = () => <>
        {
            props.editEnabled &&
            <IconButton aria-label="edit" color="default" className={"hiding-click-icon"} size="small">
                <EditIcon fontSize="small"/>
            </IconButton>
        }
    </>;

    if (!props.isEditing) {
        return <Box
            onClick={
                () => {
                    props.editEnabled && props.onEditClick()
                }}
            style={{cursor: props.editEnabled ? "pointer" : "initial"}}
            display="flex"
            alignItems="center"
            maxWidth="100%"
            className={classes.textBox}
        >
            {
                overflow ?
                    <>
                        <Tooltip title={props.initialValue}>{textHolder}</Tooltip>
                        <EditIconButton/>
                    </>
                    :
                    <>
                        {textHolder}
                        <EditIconButton/>
                    </>
            }
        </Box>
    }

    return (
        <Box flexGrow={1}>
            <MicroForm onSubmit={(v: { value: any }) => props.onSubmit(v.value)} initialValues={{value: props.initialValue || ""}}
                       validationSchema={Yup.object().shape({value: props.validationSchema})}>
                <EasyInput onChange={onInputChange} multiline name="value" fullWidth endAdornment={
                    <InputAdornment position="end">
                        <IconButton
                            edge="end"
                            color="primary"
                            type="submit"
                        >
                            <CheckIcon/>
                        </IconButton>
                        <IconButton
                            onClick={() => props.onCancel(value)}
                            edge="end"
                            color="secondary"
                        >
                            <CloseIcon/>
                        </IconButton>
                    </InputAdornment>

                }/>
            </MicroForm>
        </Box>
    )
};