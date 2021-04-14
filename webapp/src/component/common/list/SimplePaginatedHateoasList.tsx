import * as React from "react";
import {ReactNode, useEffect, useState} from "react";
import {EmptyListMessage} from "../EmptyListMessage";
import {SimplePaperList} from "./SimplePaperList";
import {AbstractLoadableActions, StateWithLoadables} from "../../../store/AbstractLoadableActions";
import {HateoasPaginatedData} from "../../../service/response.types";
import {BoxLoading} from "../BoxLoading";
import {Box, Grid, Typography} from "@material-ui/core";
import SearchField from "../form/fields/SearchField";
import {startLoading, stopLoading} from "../../../hooks/loading";

type EmbeddedDataItem<ActionsType extends AbstractLoadableActions<ResourceActionsStateType>, ResourceActionsStateType extends StateWithLoadables<ActionsType>, LoadableName extends keyof ResourceActionsStateType["loadables"]> =
    NonNullable<ResourceActionsStateType["loadables"][LoadableName]["data"]> extends HateoasPaginatedData<infer ItemDataType> ? ItemDataType : never

export interface SimplePaginatedHateoasListProps<ItemDataType, ActionsType extends AbstractLoadableActions<ResourceActionsStateType>, ResourceActionsStateType extends StateWithLoadables<any>,
    LoadableName extends keyof ActionsType["loadableDefinitions"]> {
    renderItem: (itemData: EmbeddedDataItem<ActionsType, ResourceActionsStateType, LoadableName>) => ReactNode
    actions: ActionsType
    loadableName: LoadableName
    dispatchParams?: [Omit<Parameters<ActionsType["loadableDefinitions"][LoadableName]["payloadProvider"]>[0], "query">, ...any[]]
    pageSize?: number,
    title?: ReactNode
    search?: boolean
}

export function SimplePaginatedHateoasList<ItemDataType,
    ActionsType extends AbstractLoadableActions<any>,
    ResourceActionsStateType extends StateWithLoadables<ActionsType>,
    LoadableName extends keyof ActionsType["loadableDefinitions"]>
(props: SimplePaginatedHateoasListProps<ItemDataType, ActionsType, ResourceActionsStateType, LoadableName>) {
    const loadable = props.actions.useSelector((state) => state.loadables[props.loadableName])
    const [currentPage, setCurrentPage] = useState(1)
    const [search, setSearch] = useState(undefined as string | undefined);

    const loadPage = (page: number) => {
        const [requestParam, ...otherParams] = props.dispatchParams ? [...props.dispatchParams] : [];

        const params = [{
            ...requestParam, query: {
                ...requestParam, pageable: {
                    page: page - 1,
                    size: props.pageSize || 20,
                    sort: ["name"]
                },
                search: search || undefined
            }
        }, ...otherParams] as Parameters<(typeof props.actions.loadableActions[LoadableName])["dispatch"]>

        return props.actions.loadableActions[props.loadableName].dispatch(...params)
    }

    useEffect(() => {
        if (search != undefined) {
            loadPage(1)
        }
    }, [search])

    useEffect(() => {
        if (!loadable.touched) {
            loadPage(currentPage)
        }
    }, [loadable.touched])

    useEffect(() => {
        return () => {
            (props.actions.loadableReset as any)[props.loadableName].dispatch()
        }
    }, [])

    const onPageChange = (page) => {
        setCurrentPage(page)
        loadPage(page)
    }

    const data = loadable.data as any
    const embedded = data?._embedded;
    const key = embedded ? Object.keys(embedded)?.[0] : null
    const pageCount = data ? Math.ceil(data.page?.totalElements!! / data.page?.size!!) : undefined
    const items = key ? embedded?.[key] : null

    useEffect(() => {
            //move user to last page when pageCount is less then currentPage
            if (pageCount && pageCount < currentPage) {
                setCurrentPage(pageCount)
                loadPage(pageCount)
            }
        }, [loadable.data]
    )

    useEffect(() => {
        //to trigger global loading just when data are present (when loading for the first time, BoxLoading is rendered lower)
        if (loadable.loading && loadable.data) {
            startLoading()
        }

        if (!loadable.loading) {
            stopLoading()
        }
    })

    if (loadable.loading && !loadable.data) {
        return <BoxLoading/>
    }

    return (
        <Box data-cy="global-paginated-list"> {(props.title || props.search) &&
        <Box mb={1}>
            <Grid container alignItems="center">
                <Grid item lg md sm xs>
                    {props.title &&
                    <Box><Typography variant="h6">{props.title}</Typography></Box>}
                </Grid>
                {props.search &&
                <Grid item lg={4} md={5} sm={12} xs={12}>
                    <SearchField data-cy="global-list-search" fullWidth initial={search || ""} onSearch={val => {
                        setSearch(val)
                    }} variant={"outlined"} size="small"/>
                </Grid>}
            </Grid>
        </Box>}
            {!embedded ? <EmptyListMessage/> : <SimplePaperList
                pagination={{
                    page: data.page?.number!! + 1,
                    onPageChange,
                    pageCount: pageCount!
                }}
                data={items}
                renderItem={props.renderItem}
            />}
        </Box>
    )
}
