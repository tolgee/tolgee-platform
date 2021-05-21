import React, {FunctionComponent} from "react";
import {Box, Button, Grid, Typography, useMediaQuery, useTheme} from "@material-ui/core";
import {T} from "@tolgee/react";
import {DoneAll} from "@material-ui/icons";
import {container} from "tsyringe";
import {ImportActions} from "../../../../../store/repository/ImportActions";
import {useRepository} from "../../../../../hooks/useRepository";
import {components} from "../../../../../service/apiSchema";

const actions = container.resolve(ImportActions)
export const ImportConflictsDataHeader: FunctionComponent<{
    language: components["schemas"]["ImportLanguageModel"]
}> = (props) => {
    const repository = useRepository()

    const theme = useTheme();
    const isMdOrGreater = useMediaQuery(theme.breakpoints.up('md'));
    const isSmOrLower = useMediaQuery(theme.breakpoints.down('sm'));

    const keepAllExisting = () => {
        actions.loadableActions.resolveAllKeepExisting.dispatch({
            path: {
                repositoryId: repository.id,
                languageId: props.language!.id
            }
        })
    }

    const overrideAll = () => {
        actions.loadableActions.resolveAllOverride.dispatch({
            path: {
                repositoryId: repository.id,
                languageId: props.language!.id
            }
        })
    }

    const keepAllButton = <Button fullWidth={!isMdOrGreater} startIcon={<DoneAll/>} variant="outlined" color="inherit"
                                  onClick={keepAllExisting}><T>import_resolution_accept_old</T></Button>
    const overrideAllButton = <Button fullWidth={!isMdOrGreater} startIcon={<DoneAll/>} variant="outlined" color="inherit"
                                      onClick={overrideAll}><T>import_resolution_accept_imported</T></Button>

    return (
        <Box pl={2} pt={2} pb={1} pr={2}>
            {isMdOrGreater ?
                <Grid container spacing={2}>
                    <Grid item lg={3} md>
                        <Box>
                            <Typography><b><T>import_resolve_header_key</T></b></Typography>
                        </Box>
                    </Grid>
                    <Grid item lg md sm={12} xs={12}>
                        <Box display="flex">
                            <Box pl={1} flexGrow={1}>
                                <Typography><b><T>import_resolve_header_existing</T></b></Typography>
                            </Box>
                            {keepAllButton}
                        </Box>
                    </Grid>
                    <Grid item lg md sm={12} xs={12}>
                        <Box display="flex">
                            <Box flexGrow={1}>
                                <Typography><b><T>import_resolve_header_new</T></b></Typography>
                            </Box>
                            {overrideAllButton}
                        </Box>
                    </Grid>
                </Grid>
                : (isSmOrLower &&
                    <Grid container spacing={4}>
                        <Grid item lg md sm xs>
                            {keepAllButton}
                        </Grid>
                        <Grid item lg md sm xs>
                            {overrideAllButton}
                        </Grid>
                    </Grid>)
            }
        </Box>
    )
}
