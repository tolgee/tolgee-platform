import React, {FunctionComponent} from "react";
import {SecondaryBar} from "../../../../layout/SecondaryBar";
import {Box, FormControlLabel, Grid, makeStyles, Switch, Typography} from "@material-ui/core";
import {T} from "@tolgee/react";
import {CheckCircle, Warning} from "@material-ui/icons";
import {container} from "tsyringe";
import {ImportActions} from "../../../../../store/repository/ImportActions";
import clsx from "clsx";

const useStyles = makeStyles(theme => ({
    counter: {
        display: "flex",
        alignItems: "center",
        borderRadius: 20
    },
    icon: {
        fontSize: 20,
        marginRight: 4,
    },
    resolvedIcon: {
        color: theme.palette.success.main
    },
    conflictsIcon: {
        marginLeft: theme.spacing(2),
        color: theme.palette.warning.main
    }
}))


const actions = container.resolve(ImportActions)
export const ImportConflictsSecondaryBar: FunctionComponent<{
    onShowResolvedToggle: () => void,
    showResolved: boolean
}> = (props) => {
    const languageDataLoadable = actions.useSelector(s => s.loadables.resolveConflictsLanguage)

    const classes = useStyles()

    return (
        <SecondaryBar>
            <Grid container spacing={4}>
                <Grid item alignItems="center">
                    {languageDataLoadable.data &&
                    <Box className={classes.counter}>
                        <CheckCircle className={clsx(classes.icon, classes.resolvedIcon)}/>

                        <Typography variant="body1">
                            {languageDataLoadable.data.resolvedCount}
                        </Typography>

                        <Warning className={clsx(classes.icon, classes.conflictsIcon)}/>

                        <Typography variant="body1">
                            {languageDataLoadable.data.conflictCount}
                        </Typography>
                    </Box>
                    }
                </Grid>
                <Grid item>
                    <FormControlLabel
                        control={
                            <Switch
                                checked={props.showResolved}
                                onChange={props.onShowResolvedToggle}
                                name="filter_unresolved"
                                color="primary"
                            />
                        }
                        label={<T>import_conflicts_filter_show_resolved_label</T>}
                    />
                </Grid>
            </Grid>
        </SecondaryBar>
    )
}
