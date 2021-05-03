import React, {FunctionComponent} from 'react';
import {createStyles, makeStyles, Theme} from '@material-ui/core/styles';
import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import IconButton from '@material-ui/core/IconButton';
import Typography from '@material-ui/core/Typography';
import CloseIcon from '@material-ui/icons/Close';
import Slide from '@material-ui/core/Slide';
import {TransitionProps} from '@material-ui/core/transitions';
import {components} from "../../../../../service/apiSchema";
import {T} from "@tolgee/react";
import {ImportConflictsData} from "./ImportConflictsData";
import {Box} from "@material-ui/core";

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        appBar: {
            position: 'relative',
        },
        title: {
            marginLeft: theme.spacing(2),
            flex: 1,
        },
    }),
);

const Transition = React.forwardRef(function Transition(
    props: TransitionProps & { children?: React.ReactElement },
    ref: React.Ref<unknown>,
) {
    return <Slide direction="up" ref={ref} {...props} />;
});

export const ImportConflictResolutionDialog: FunctionComponent<{
    row?: components["schemas"]["ImportLanguageModel"]
    onClose: () => void
}> = (props) => {
    const classes = useStyles();


    return (
        <div>
            <Dialog fullScreen open={!!props.row} onClose={props.onClose} TransitionComponent={Transition}>
                <AppBar className={classes.appBar}>
                    <Toolbar>
                        <IconButton edge="start" color="inherit" onClick={props.onClose} aria-label="close">
                            <CloseIcon/>
                        </IconButton>
                        <Typography variant="h6" className={classes.title}>
                            <T>import_resolve_conflicts_title</T>
                        </Typography>

                        <Box mr={2}>
                            <Button color="inherit"><T>import_resolution_accept_old</T></Button>
                        </Box>
                        <Button color="inherit"><T>import_resolution_accept_imported</T></Button>
                    </Toolbar>
                </AppBar>
                {!!props.row &&
                <ImportConflictsData row={props.row}/>}
            </Dialog>
        </div>
    );
}
