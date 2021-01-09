import * as React from 'react';
import {FunctionComponent, useContext, useState} from 'react';
import {Box, TablePagination} from "@material-ui/core";
import {TranslationListContext} from "./TtranslationsGridContextProvider";
import {T} from "@polygloat/react";
import {useLeaveEditConfirmationPagination} from "./useLeaveEditConfirmation";

export const Pagination: FunctionComponent = (props) => {
    const listContext = useContext(TranslationListContext);

    const [perPage, setPerPage] = useState(listContext.perPage);
    const [page, setPage] = useState(Math.ceil(listContext.listLoadable.data.paginationMeta.offset / listContext.perPage));

    const confirmation = useLeaveEditConfirmationPagination();

    const onPerPageChange = (pp) => {
        confirmation(() => {
            setPerPage(pp);
            setPage(0);
            listContext.loadData(listContext.listLoadable.data.params.search, pp, 0);
        })
    };

    const onPageChange = (p) => {
        confirmation(() => {
            setPage(p);
            listContext.loadData(listContext.listLoadable.data.params.search, perPage, p * perPage);
        })
    };

    return (
        <Box mt={3}>
            <Box display="flex" justifyContent="flex-end">
                <TablePagination component={Box} rowsPerPageOptions={[10, 20, 30, 40, 50]}
                                 count={listContext.listLoadable.data.paginationMeta.allCount}
                                 onChangePage={(_, p) => onPageChange(p)}
                                 page={page}
                                 onChangeRowsPerPage={(e) => onPerPageChange(e.target.value)}
                                 rowsPerPage={perPage}
                                 labelRowsPerPage={<T>pagination_rows_per_page</T>}
                                 labelDisplayedRows={(props) =>
                                     <T parameters={
                                         Object.entries(props).reduce((acc: object, curr) => ({...acc, [curr[0]]: curr[1].toString()}), {})
                                     }>pagination_displayed_rows</T>}
                />
            </Box>
        </Box>
    );
};