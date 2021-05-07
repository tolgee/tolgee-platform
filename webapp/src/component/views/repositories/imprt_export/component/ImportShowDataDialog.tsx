import React, {FunctionComponent} from 'react';
import {createStyles, makeStyles, Theme} from '@material-ui/core/styles';
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
import {Box, Grid} from "@material-ui/core";
import {container} from "tsyringe";
import {ImportActions} from "../../../../../store/repository/ImportActions";
import {useRepository} from "../../../../../hooks/useRepository";
import {SimplePaginatedHateoasList} from "../../../../common/list/SimplePaginatedHateoasList";

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

export const ImportShowDataDialog: FunctionComponent<{
    row?: components["schemas"]["ImportLanguageModel"]
    onClose: () => void
}> = (props) => {
    const classes = useStyles();
    const repository = useRepository()

    return (
        <div>
            <Dialog fullScreen open={!!props.row} onClose={props.onClose} TransitionComponent={Transition}>
                <AppBar className={classes.appBar}>
                    <Toolbar>
                        <IconButton edge="start" color="inherit" onClick={props.onClose} aria-label="close">
                            <CloseIcon/>
                        </IconButton>
                        <Typography variant="h6" className={classes.title}>
                            <T>import_show_translations_title</T>
                        </Typography>
                    </Toolbar>
                </AppBar>
                {!!props.row &&
                <SimplePaginatedHateoasList
                    actions={actions} loadableName="translations"
                    sortBy={[]}
                    dispatchParams={[
                        {
                            path: {
                                languageId: props.row?.id!,
                                repositoryId: repository.id
                            },
                            query: {
                                onlyConflicts: false,
                            }
                        }
                    ]}
                    renderItem={i =>
                        <Box pt={1} pl={2} pr={2}>
                            <Grid container>
                                <Grid item lg md sm xs>
                                    <Box>
                                        <Typography
                                            style={{
                                                wordBreak: "break-all"
                                            }}
                                            variant="body1">{i.keyName}</Typography>
                                    </Box>
                                </Grid>
                                <Grid item lg md sm xs>
                                    <Typography variant="body1">{i.text}</Typography>
                                </Grid>
                            </Grid>
                        </Box>
                    }
                />}
            </Dialog>
        </div>
    );
}
