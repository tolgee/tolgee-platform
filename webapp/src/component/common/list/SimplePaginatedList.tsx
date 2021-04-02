import {HateoasPaginatedData} from "../../../service/response.types";
import * as React from "react";
import {ReactNode} from "react";
import {Pagination} from "@material-ui/lab";
import List from "@material-ui/core/List";
import {EmptyListMessage} from "../EmptyListMessage";
import {Box} from "@material-ui/core";

export interface SimplePaginatedListProps<ItemDataType> {
    data: HateoasPaginatedData<ItemDataType>
    renderItem: (itemData: ItemDataType) => ReactNode
}

export function SimplePaginatedList<ItemDataType>(props: SimplePaginatedListProps<ItemDataType>) {
    const embedded = props.data?._embedded;
    if (!embedded) {
        return (
            <>
                <EmptyListMessage/>
            </>
        )
    }

    const key = Object.keys(embedded)[0];

    const pageCount = Math.ceil(props.data.page?.totalElements!! / props.data.page?.size!!);

    return (
        <>
            <List>
                {embedded[key].map(r => props.renderItem(r))}
            </List>

            {pageCount > 1 &&
            <Box display="flex" justifyContent="flex-end">
                <Pagination page={props.data.page?.number} count={pageCount}/>
            </Box>}
        </>
    )
}
