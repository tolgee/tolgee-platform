import * as React from "react";
import {ReactNode, useEffect} from "react";
import {EmptyListMessage} from "../EmptyListMessage";
import {SimplePaperList} from "./SimplePaperList";
import {AbstractLoadableActions, StateWithLoadables} from "../../../store/AbstractLoadableActions";
import {HateoasPaginatedData} from "../../../service/response.types";
import {BoxLoading} from "../BoxLoading";

type EmbeddedDataItem<ResourceActionsStateType extends StateWithLoadables<any>, LoadableName extends keyof ResourceActionsStateType["loadables"]> =
    NonNullable<ResourceActionsStateType["loadables"][LoadableName]["data"]> extends HateoasPaginatedData<infer ItemDataType> ? ItemDataType : never

export interface SimplePaginatedListProps<ItemDataType, ActionsType extends AbstractLoadableActions<ResourceActionsStateType>, ResourceActionsStateType extends StateWithLoadables<any>,
    LoadableName extends keyof ActionsType["loadableActions"]> {
    renderItem: (itemData: EmbeddedDataItem<ResourceActionsStateType, LoadableName>) => ReactNode
    actions: AbstractLoadableActions<ResourceActionsStateType>
    loadableName: LoadableName
    dispatchParams?: any[]
}

export function SimplePaginatedHateoasList<ItemDataType,
    ActionsType extends AbstractLoadableActions<ResourceActionsStateType>,
    ResourceActionsStateType extends StateWithLoadables<any>,
    LoadableName extends keyof ActionsType["loadableActions"]>
(props: SimplePaginatedListProps<ItemDataType, ActionsType, ResourceActionsStateType, LoadableName>) {

    const loadable = props.actions.useSelector((state) => state.loadables[props.loadableName])


    const loadPage = (page) => {
        const dispatchParams = props.dispatchParams ? [props.dispatchParams] : [];

        const params = [...dispatchParams, {
            page: page - 1,
            size: 5,
            sort: ["name"]
        }]

        return (props.actions.loadableActions as any)[props.loadableName].dispatch(...params)
    }

    useEffect(() => {
        if(!loadable.touched){
            loadPage(1)
        }
    }, [loadable.touched])

    useEffect(() => {
        return () => {
            (props.actions.loadableReset as any)[props.loadableName].dispatch()
        }
    }, [])

    const onPageChange = (page) => {
        loadPage(page)
    }

    const data = loadable.data as any
    const embedded = data?._embedded;

    if (loadable.loading) {
        return <BoxLoading/>
    }

    if (!embedded) {
        return (
            <>
                <EmptyListMessage/>
            </>
        )
    }

    const key = Object.keys(embedded)[0];

    const pageCount = Math.ceil(data.page?.totalElements!! / data.page?.size!!);

    return (
        <SimplePaperList
            pagination={{
                page: data.page?.number!! + 1,
                onPageChange,
                pageCount
            }}
            data={embedded[key]}
            renderItem={props.renderItem}
        />
    )
}
