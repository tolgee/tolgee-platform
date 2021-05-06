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
import {startLoading, stopLoading} from "../../../../../hooks/loading";

const actions = container.resolve(ImportActions)
export const ImportConflictsData: FunctionComponent<{
    row: components["schemas"]["ImportLanguageModel"]
}> = (props) => {
    const conflictsLoadable = actions.useSelector(s => s.loadables.conflicts)
    const repository = useRepository()
    const languageId = props.row.id
    const [showResolved, setOnlyUnresolved] = useState(props.row.resolvedCount == props.row.conflictCount)

    const setOverrideLoadable = actions.useSelector(s => s.loadables.resolveTranslationConflictOverride)
    const setKeepLoadable = actions.useSelector(s => s.loadables.resolveTranslationConflictKeep)

    const loadData = (page = 0) => {
        actions.loadableActions.conflicts.dispatch(
            {
                path: {
                    languageId: languageId,
                    repositoryId: repository.id
                },
                query: {
                    onlyConflicts: true,
                    onlyUnresolved: !showResolved,
                    pageable: {
                        page: page,
                        size: 50
                    }
                }
            }
        )
    }

    const setOverride = (translationId: number) => {
        actions.loadableActions.resolveTranslationConflictOverride.dispatch({
            path: {
                repositoryId: repository.id,
                languageId: languageId,
                translationId: translationId
            }
        })
    }

    const setKeepExisting = (translationId: number) => {
        actions.loadableActions.resolveTranslationConflictKeep.dispatch({
            path: {
                repositoryId: repository.id,
                languageId: languageId,
                translationId: translationId
            }
        })
    }

    const data = conflictsLoadable.data?._embedded?.translations
    const totalPages = conflictsLoadable.data?.page?.totalPages
    const pageSize = conflictsLoadable.data?.page?.size
    const page = conflictsLoadable.data?.page?.number

    useEffect(() => {
        loadData(0)
    }, [props.row, showResolved])

    useEffect(() => {
        if (setOverrideLoadable.loaded || setKeepLoadable.loaded) {
            setTimeout(() => {
                loadData(page)
            }, 300)
        }
    }, [setOverrideLoadable.loading, setKeepLoadable.loading])

    useEffect(() => {
        if (!conflictsLoadable.loading) {
            stopLoading()
            actions.loadableReset.resolveTranslationConflictKeep.dispatch()
            actions.loadableReset.resolveTranslationConflictOverride.dispatch()
            return
        }
        startLoading()
    }, [conflictsLoadable.loading])

    if (!conflictsLoadable.loaded) {
        return <BoxLoading/>
    }

    const isKeepExistingLoading = (translationId) => setKeepLoadable.dispatchParams?.[0].path.translationId === translationId && setKeepLoadable.loading
    const isOverrideLoading = (translationId) => setOverrideLoadable.dispatchParams?.[0].path.translationId === translationId && setOverrideLoadable.loading
    const isKeepExistingLoaded = (translationId) => setKeepLoadable.dispatchParams?.[0].path.translationId === translationId && setKeepLoadable.loaded
    const isOverrideLoaded = (translationId) => setOverrideLoadable.dispatchParams?.[0].path.translationId === translationId && setOverrideLoadable.loaded

    return (
        <>
            <Box p={2}>
                <FormControlLabel
                    control={
                        <Switch
                            checked={showResolved}
                            onChange={() => setOnlyUnresolved(!showResolved)}
                            name="filter_unresolved"
                            color="primary"
                        />
                    }
                    label={<T>import_conflicts_filter_show_resolved_label</T>}
                />
            </Box>
            {conflictsLoadable.loaded && (data ?
                <>
                    {data.map(t =>
                        <Box pt={2} pb={1} pl={2} pr={2}>
                            <Grid container spacing={2}>
                                <Grid item lg={3} md>
                                    <Box p={1}>
                                        <Typography style={{overflowWrap: "break-word"}} variant={"body2"}><b>{t.keyName}</b></Typography>
                                    </Box>
                                </Grid>
                                {t.conflictId &&
                                <Grid item lg md sm={12} xs={12}>
                                    <ImportConflictTranslation
                                        loading={isKeepExistingLoading(t.id)}
                                        loaded={isKeepExistingLoaded(t.id)}
                                        text={t.conflictText!}
                                        selected={!t.override && t.resolved}
                                        onSelect={() => setKeepExisting(t.id)}
                                    />
                                </Grid>}
                                <Grid item lg md sm={12} xs={12}>
                                    <ImportConflictTranslation
                                        loading={isOverrideLoading(t.id)}
                                        loaded={isOverrideLoaded(t.id)}
                                        text={t.text}
                                        selected={(t.override && t.resolved) || !t.conflictId}
                                        onSelect={() => setOverride(t.id)}
                                    />
                                </Grid>
                            </Grid>
                        </Box>)}
                </>
                :
                <EmptyListMessage><T>import_resolve_conflicts_empty_list_message</T></EmptyListMessage>)
            }
            <Box display="flex" justifyContent="flex-end" p={4}>
                {totalPages!! > 1 &&
                <Pagination
                    page={page!! + 1}
                    count={totalPages}
                    onChange={(_, page) => loadData(page - 1)}
                />}
            </Box>
        </>
    );
}
