import React, { FunctionComponent, useState } from 'react';
import { Box, Grid, Pagination, Typography } from '@mui/material';
import { T } from '@tolgee/react';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { components } from 'tg.service/apiSchema.generated';

import { ImportConflictTranslationsPair } from './ImportConflictTranslationsPair';
import { ImportConflictsDataHeader } from './ImportConflictsDataHeader';
import { ImportConflictsSecondaryBar } from './ImportConflictsSecondaryBar';
import { useConflictsHelper } from '../hooks/useConflictsHelper';

export const ImportConflictsData: FunctionComponent<{
  row: components['schemas']['ImportLanguageModel'];
}> = (props) => {
  const languageId = props.row.id;
  const [showResolved, setShowResolved] = useState(true);
  const [page, setPage] = useState(0);

  const helper = useConflictsHelper({ languageId, showResolved, page });

  const data = helper.conflictsLoadable.data?._embedded?.translations;
  const totalPages = helper.conflictsLoadable.data?.page?.totalPages;

  if (
    !helper.conflictsLoadable.isFetched ||
    !helper.languageLoadable.isFetched
  ) {
    return <BoxLoading />;
  }

  return (
    <>
      <ImportConflictsSecondaryBar
        showResolved={showResolved}
        onShowResolvedToggle={() => setShowResolved(!showResolved)}
        languageData={helper.languageLoadable.data!}
      />
      {data ? (
        <>
          <ImportConflictsDataHeader language={props.row} />
          {data.map((t) => (
            <Box
              pt={1}
              pb={1}
              pl={2}
              pr={2}
              key={t.id}
              data-cy="import-resolution-dialog-data-row"
            >
              <Grid container spacing={2}>
                <Grid item lg={3} md>
                  <Box p={1}>
                    <Typography
                      style={{ overflowWrap: 'break-word' }}
                      variant={'body2'}
                      data-cy="import-resolution-dialog-key-name"
                    >
                      <b>{t.keyName}</b>
                    </Typography>
                  </Box>
                </Grid>
                <ImportConflictTranslationsPair
                  translation={t}
                  row={props.row}
                />
              </Grid>
            </Box>
          ))}
        </>
      ) : (
        <EmptyListMessage>
          <T keyName="import_resolve_conflicts_empty_list_message" />
        </EmptyListMessage>
      )}
      <Box display="flex" justifyContent="flex-end" p={4}>
        {totalPages! > 1 && (
          <Pagination
            data-cy="global-list-pagination"
            page={page! + 1}
            count={totalPages}
            onChange={(_, page) => setPage(page - 1)}
          />
        )}
      </Box>
    </>
  );
};
