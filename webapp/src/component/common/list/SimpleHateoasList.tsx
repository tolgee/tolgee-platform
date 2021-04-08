import * as React from "react";
import {ReactNode, useEffect} from "react";
import {EmptyListMessage} from "../EmptyListMessage";
import {SimplePaperList} from "./SimplePaperList";
import {AbstractLoadableActions, StateWithLoadables} from "../../../store/AbstractLoadableActions";
import {HateoasListData} from "../../../service/response.types";
import {BoxLoading} from "../BoxLoading";

type EmbeddedDataItem<ResourceActionsStateType extends StateWithLoadables<any>, LoadableName extends keyof ResourceActionsStateType["loadables"]> =
    NonNullable<ResourceActionsStateType["loadables"][LoadableName]["data"]> extends HateoasListData<infer ItemDataType> ? ItemDataType : never

export interface SimpleHateoasListProps<ItemDataType, ActionsType extends AbstractLoadableActions<ResourceActionsStateType>, ResourceActionsStateType extends StateWithLoadables<any>,
    LoadableName extends keyof ActionsType["loadableActions"]> {
    renderItem: (itemData: EmbeddedDataItem<ResourceActionsStateType, LoadableName>) => ReactNode
    actions: AbstractLoadableActions<ResourceActionsStateType>
    loadableName: LoadableName
    dispatchParams?: any[]
    pageSize?: number
}

export function SimpleHateoasList<ItemDataType,
    ActionsType extends AbstractLoadableActions<ResourceActionsStateType>,
    ResourceActionsStateType extends StateWithLoadables<any>,
    LoadableName extends keyof ActionsType["loadableActions"]>
(props: SimpleHateoasListProps<ItemDataType, ActionsType, ResourceActionsStateType, LoadableName>) {
    const loadable = props.actions.useSelector((state) => state.loadables[props.loadableName])

    useEffect(() => {
        if(!loadable.touched){
            const dispatchParams = props.dispatchParams ? [...props.dispatchParams] : [];
            return (props.actions.loadableActions as any)[props.loadableName].dispatch(dispatchParams)
        }
    }, [loadable.touched])

    useEffect(() => {
        return () => {
            (props.actions.loadableReset as any)[props.loadableName].dispatch()
        }
    }, [])

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

    return (
        <SimplePaperList
            data={embedded[key]}
            renderItem={props.renderItem}
        />
    )
}
