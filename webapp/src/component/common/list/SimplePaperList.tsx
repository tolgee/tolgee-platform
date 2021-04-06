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

export function SimplePaperList<T>(props: SimplePaperListProps<T>) {
    return (
        <>
            <Paper variant="outlined">
                <List>
                    {props.data.map((i) => props.renderItem(i))}
                </List>
                {props.pagination && props.pagination.pageCount > 1 &&
                <Box display="flex" justifyContent="flex-end" mt={1} mb={1}>
                    <Pagination page={props.pagination.page}
                                count={props.pagination.pageCount}
                                onChange={(_, value) => props.pagination?.onPageChange(value)}
                    />
                </Box>}
            </Paper>
        </>
    );
}
