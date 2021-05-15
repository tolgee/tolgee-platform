import React, {FunctionComponent, useEffect} from 'react';
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
import {container} from "tsyringe";
import {ImportActions} from "../../../../../store/repository/ImportActions";
import {useRepository} from "../../../../../hooks/useRepository";

const actions = container.resolve(ImportActions)
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
    const repository = useRepository()

    const keepAllExistingLoadable = actions.useSelector(s => s.loadables.resolveAllKeepExisting)
    const overrideAllLoadable = actions.useSelector(s => s.loadables.resolveAllOverride)

    const keepAllExisting = () => {
        actions.loadableActions.resolveAllKeepExisting.dispatch({
            path: {
                repositoryId: repository.id,
                languageId: props.row!.id
            }
        })
    }

    const overrideAll = () => {
        actions.loadableActions.resolveAllOverride.dispatch({
            path: {
                repositoryId: repository.id,
                languageId: props.row!.id
            }
        })
    }

    useEffect(() => {
        if (keepAllExistingLoadable.loaded || overrideAllLoadable.loading) {
            props.onClose()
            actions.loadableReset.resolveAllKeepExisting.dispatch()
            actions.loadableReset.resolveAllOverride.dispatch()
        }
    }, [keepAllExistingLoadable.loading, overrideAllLoadable.loading])

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
                            <Button color="inherit" onClick={keepAllExisting}><T>import_resolution_accept_old</T></Button>
                        </Box>
                        <Button color="inherit" onClick={overrideAll}><T>import_resolution_accept_imported</T></Button>
                    </Toolbar>
                </AppBar>
                {!!props.row &&
                <ImportConflictsData row={props.row}/>}
            </Dialog>
        </div>
    );
}
