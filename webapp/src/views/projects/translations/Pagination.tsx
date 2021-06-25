import { FunctionComponent, useContext, useState } from 'react';
import { Box, TablePagination } from '@material-ui/core';
import { TranslationListContext } from './TtranslationsGridContextProvider';
import { T } from '@tolgee/react';
import { useLeaveEditConfirmationPagination } from './useLeaveEditConfirmation';

export const Pagination: FunctionComponent = (props) => {
  const listContext = useContext(TranslationListContext);

  const [perPage, setPerPage] = useState(listContext.perPage);
  const confirmation = useLeaveEditConfirmationPagination();

  const onPerPageChange = (perPage) => {
    confirmation(() => {
      setPerPage(perPage);
      listContext.loadData(
        listContext.listLoadable!.data!.params!.search,
        perPage,
        0
      );
    });
  };

  const onPageChange = (p) => {
    confirmation(() => {
      listContext.loadData(
        listContext.listLoadable!.data!.params!.search,
        perPage,
        p * perPage
      );
    });
  };

  const page = Math.ceil(
    Number(listContext.listLoadable!.data!.paginationMeta!.offset) /
      listContext.perPage
  );

  return (
    <Box mt={3}>
      <Box display="flex" justifyContent="flex-end">
        <TablePagination
          component={Box}
          rowsPerPageOptions={[10, 20, 30, 40, 50]}
          count={
            listContext.listLoadable!.data!.paginationMeta!.allCount as number
          }
          onChangePage={(_, p) => onPageChange(p)}
          page={page}
          onChangeRowsPerPage={(e) => onPerPageChange(e.target.value)}
          rowsPerPage={perPage}
          labelRowsPerPage={<T>pagination_rows_per_page</T>}
          labelDisplayedRows={(props) => (
            <T
              parameters={Object.entries(props).reduce(
                (acc, curr) => ({
                  ...acc,
                  [curr[0]]: curr[1].toString(),
                }),
                {}
              )}
            >
              pagination_displayed_rows
            </T>
          )}
        />
      </Box>
    </Box>
  );
};
