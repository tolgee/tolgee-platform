import React, { FunctionComponent, useEffect, useState } from 'react';
import { Box, Grid, Typography } from '@material-ui/core';
import { Pagination } from '@material-ui/lab';
import { T } from '@tolgee/react';
import { container } from 'tsyringe';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { startLoading, stopLoading } from 'tg.hooks/loading';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { ImportActions } from 'tg.store/project/ImportActions';

import { ImportConflictTranslationsPair } from './ImportConflictTranslationsPair';
import { ImportConflictsDataHeader } from './ImportConflictsDataHeader';
import { ImportConflictsSecondaryBar } from './ImportConflictsSecondaryBar';

const actions = container.resolve(ImportActions);
export const ImportConflictsData: FunctionComponent<{
  row: components['schemas']['ImportLanguageModel'];
}> = (props) => {
  const conflictsLoadable = actions.useSelector((s) => s.loadables.conflicts);
  const project = useProject();
  const languageId = props.row.id;
  const [showResolved, setShowResolved] = useState(true);
  const setOverrideLoadable = actions.useSelector(
    (s) => s.loadables.resolveTranslationConflictOverride
  );
  const setKeepLoadable = actions.useSelector(
    (s) => s.loadables.resolveTranslationConflictKeep
  );

  const loadData = (page = 0) => {
    actions.loadableActions.conflicts.dispatch({
      path: {
        languageId: languageId,
        projectId: project.id,
      },
      query: {
        onlyConflicts: true,
        onlyUnresolved: !showResolved,
        page: page,
        size: 50,
      },
    });
    actions.loadableActions.resolveConflictsLanguage.dispatch({
      path: {
        languageId: languageId,
        projectId: project.id,
      },
    });
  };

  const data = conflictsLoadable.data?._embedded?.translations;
  const totalPages = conflictsLoadable.data?.page?.totalPages;
  const page = conflictsLoadable.data?.page?.number;

  const keepAllExistingLoadable = actions.useSelector(
    (s) => s.loadables.resolveAllKeepExisting
  );
  const overrideAllLoadable = actions.useSelector(
    (s) => s.loadables.resolveAllOverride
  );

  useEffect(() => {
    if (keepAllExistingLoadable.loaded || overrideAllLoadable.loaded) {
      actions.loadableReset.resolveAllKeepExisting.dispatch();
      actions.loadableReset.resolveAllOverride.dispatch();
      loadData();
    }
  }, [keepAllExistingLoadable.loading, overrideAllLoadable.loading]);

  useEffect(() => {
    loadData(0);
  }, [props.row, showResolved]);

  useEffect(() => {
    if (setOverrideLoadable.loaded || setKeepLoadable.loaded) {
      setTimeout(() => {
        loadData(page);
      }, 300);
    }
  }, [setOverrideLoadable.loading, setKeepLoadable.loading]);

  useEffect(() => {
    return () => {
      actions.loadableReset.conflicts.dispatch();
      actions.loadableReset.resolveConflictsLanguage.dispatch();
    };
  }, []);

  useEffect(() => {
    if (!conflictsLoadable.loading) {
      stopLoading();
      actions.loadableReset.resolveTranslationConflictKeep.dispatch();
      actions.loadableReset.resolveTranslationConflictOverride.dispatch();
      return;
    }
    startLoading();
  }, [conflictsLoadable.loading]);

  if (!conflictsLoadable.loaded) {
    return <BoxLoading />;
  }

  return (
    <>
      <ImportConflictsSecondaryBar
        showResolved={showResolved}
        onShowResolvedToggle={() => setShowResolved(!showResolved)}
      />
      {conflictsLoadable.loaded &&
        (data ? (
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
                    languageId={languageId}
                  />
                </Grid>
              </Box>
            ))}
          </>
        ) : (
          <EmptyListMessage>
            <T>import_resolve_conflicts_empty_list_message</T>
          </EmptyListMessage>
        ))}
      <Box display="flex" justifyContent="flex-end" p={4}>
        {totalPages! > 1 && (
          <Pagination
            data-cy="global-list-pagination"
            page={page! + 1}
            count={totalPages}
            onChange={(_, page) => loadData(page - 1)}
          />
        )}
      </Box>
    </>
  );
};
