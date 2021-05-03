import React from 'react';
import {Box, CircularProgress, makeStyles} from "@material-ui/core";
import clsx from "clsx";
import {green} from "@material-ui/core/colors";
import CheckIcon from "@material-ui/icons/Check";

type ImportConflictTranslationProps = {
    text: string,
    selected?: boolean,
    onSelect?: () => void,
    loading?: boolean
    loaded?: boolean
}

const useStyles = makeStyles(theme => ({
    root: {
        borderRadius: theme.shape.borderRadius,
        border: `1px dashed ${theme.palette.grey.A100}`,
        padding: theme.spacing(1),
        cursor: "pointer"
    },
    selected: {
        borderColor: green["900"],
        backgroundColor: green["50"]
    },
    loading: {
        position: "absolute",
        right: 0,
        top: 0
    }
}))

export const ImportConflictTranslation = (props: ImportConflictTranslationProps) => {
    const classes = useStyles()

    return (
        <Box position="relative" onClick={props.onSelect} className={clsx(classes.root, {[classes.selected]: props.selected || props.loaded})}>
            {props.loading &&
            <Box className={classes.loading} p={1}>
                <CircularProgress size={20}/>
            </Box>
            }
            {props.loaded &&
            <Box className={classes.loading} p={1}>
                <CheckIcon/>
            </Box>
            }
            {props.text}
        </Box>
    );
};
