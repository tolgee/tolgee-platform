import React from 'react';
import {Box, makeStyles} from "@material-ui/core";
import clsx from "clsx";
import {green} from "@material-ui/core/colors";

type ImportConflictTranslationProps = {
    text: string,
    selected?: boolean,
}

const useStyles = makeStyles(theme => ({
    root: {
        borderRadius: theme.shape.borderRadius,
        border: `1px dashed ${theme.palette.grey.A100}`,
        padding: theme.spacing(1)
    },
    selected: {
        borderColor: green["900"],
        backgroundColor: green["50"]
    }
}))

export const ImportConflictTranslation = (props: ImportConflictTranslationProps) => {
    const classes = useStyles()

    return (
        <Box className={clsx(classes.root, {[classes.selected]: props.selected})}>
            {props.text}
        </Box>
    );
};
