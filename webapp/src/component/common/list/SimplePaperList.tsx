import React, {PropsWithChildren, ReactNode} from 'react';
import {Box, List, Paper} from "@material-ui/core";
import {Pagination} from "@material-ui/lab";

type SimplePaperListProps<T> = {
    data: T[]
    pagination?: {
        page: number
        pageCount: number
        onPageChange: (page: number) => void
    }
    renderItem: (item: T) => ReactNode
}

export function SimplePaperList<T>(props: SimplePaperListProps<T> & React.ComponentProps<typeof Paper>) {
    const {data, pagination, renderItem, ...paperProps} = props

    return (
        <>
            <Paper variant="outlined" {...paperProps}>
                <List>
                    {data.map((i) => props.renderItem(i))}
                </List>
                {pagination && pagination.pageCount > 1 &&
                <Box display="flex" justifyContent="flex-end" mt={1} mb={1}>
                    <Pagination data-cy="global-list-pagination"
                                page={pagination.page}
                                count={pagination.pageCount}
                                onChange={(_, value) => props.pagination?.onPageChange(value)}
                    />
                </Box>}
            </Paper>
        </>
    );
}
