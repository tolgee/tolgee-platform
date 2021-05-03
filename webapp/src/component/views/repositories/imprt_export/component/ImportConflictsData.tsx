import React, {FunctionComponent, useEffect, useState} from "react";
import {components} from "../../../../../service/apiSchema";
import {container} from "tsyringe";
import {ImportActions} from "../../../../../store/repository/ImportActions";
import {useRepository} from "../../../../../hooks/useRepository";
import {Box, FormControlLabel, Grid, Switch, Typography} from "@material-ui/core";
import {ImportConflictTranslation} from "./ImportConflictTranslation";
import {BoxLoading} from "../../../../common/BoxLoading";
import {EmptyListMessage} from "../../../../common/EmptyListMessage";
import {T} from "@tolgee/react";
import {Pagination} from "@material-ui/lab";

const actions = container.resolve(ImportActions)
export const ImportConflictsData: FunctionComponent<{
    row: components["schemas"]["ImportLanguageModel"]
}> = (props) => {
    const conflictsLoadable = actions.useSelector(s => s.loadables.conflicts)
    const repository = useRepository()
    const languageId = props.row.id
    const [onlyResolved, setOnlyResolved] = useState(true)

    const loadData = (page = 0) => {
        actions.loadableActions.conflicts.dispatch(
            {
                path: {
                    languageId: languageId,
                    repositoryId: repository.id
                },
                query: {
                    onlyConflicts: true,
                    pageable: {
                        page: page,
                        size: 50
                    }
                }
            }
        )
    }

    useEffect(() => {
        loadData(0)
    }, [props.row])

    if (!conflictsLoadable.loaded) {
        return <BoxLoading/>
    }

    const data = conflictsLoadable.data?._embedded?.translations
    const totalPages = conflictsLoadable.data?.page?.totalPages
    const pageSize = conflictsLoadable.data?.page?.size
    const page = conflictsLoadable.data?.page?.number

    return (
        <>
            {conflictsLoadable.loaded && (data ?

                <>
                    <Box p={2}>
                        <FormControlLabel
                            control={
                                <Switch
                                    checked={onlyResolved}
                                    onChange={() => setOnlyResolved(!onlyResolved)}
                                    name="filter_unresolved"
                                    color="primary"
                                />
                            }
                            label={<T>import_conflicts_filter_only_unresolved_label</T>}
                        />
                    </Box>
                    {data.map(t =>
                        <Box pt={2} pb={1} pl={2} pr={2}>
                            <Grid container spacing={4}>
                                <Grid item lg md>
                                    <Typography variant={"body2"}><b>{t.keyName}</b></Typography>
                                </Grid>
                            </Grid>
                            <Grid container spacing={2}>
                                {t.conflictId &&
                                <Grid item lg md sm={12} xs={12}>
                                    <ImportConflictTranslation text={t.conflictText!} selected={!t.override && t.resolved}/>
                                </Grid>}
                                <Grid item lg md sm={12} xs={12}>
                                    <ImportConflictTranslation text={t.text} selected={(t.override && t.resolved) || !t.conflictId}/>
                                </Grid>
                            </Grid>
                        </Box>)}
                </>
                :
                <EmptyListMessage><T>import_resolve_conflicts_empty_list_message</T></EmptyListMessage>)
            }
            {totalPages!! > 1 &&
            <Pagination
                page={page!! + 1}
                count={totalPages}
                onChange={(_, page) => loadData(page - 1)}
            />}
        </>
    );
}
