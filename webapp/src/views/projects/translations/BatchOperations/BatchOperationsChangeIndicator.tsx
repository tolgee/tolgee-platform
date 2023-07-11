import { Alert, Box, Button, Portal } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useEffect, useState } from 'react';
import { useProjectContext } from 'tg.hooks/ProjectContext';
import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import { END_STATUSES } from './OperationsSummary/utils';

export const BatchOperationsChangeIndicator = () => {
  const { t } = useTranslate();
  const { refetchTranslations } = useTranslationsActions();
  const lastJob = useProjectContext((c) => {
    return c.batchOperations
      ? c.batchOperations.find((o) => END_STATUSES.includes(o.status))?.id
      : 'not-loaded';
  });
  const translations = useTranslationsSelector((c) => c.translations);
  const translationsFetching = useTranslationsSelector((c) => c.isFetching);

  const [previousLast, setPreviousLast] = useState(lastJob);
  const [dataChanged, setDataChanged] = useState(false);

  function handleRefetch() {
    refetchTranslations();
  }

  // check when job is finished
  useEffect(() => {
    if (
      previousLast !== 'not-loaded' &&
      lastJob !== undefined &&
      lastJob !== previousLast
    ) {
      setDataChanged(true);
    }
    setPreviousLast(lastJob);
  }, [lastJob]);

  // reset outdated status when data are updated
  useEffect(() => {
    setDataChanged(false);
  }, [translations, translationsFetching]);

  return (
    <>
      {dataChanged && (
        <Portal>
          <Box sx={{ zIndex: 10000, position: 'fixed', right: 10, top: 60 }}>
            <Alert
              elevation={1}
              severity="warning"
              action={
                <Button color="inherit" size="small" onClick={handleRefetch}>
                  {t('batch_operations_refresh_button')}
                </Button>
              }
            >
              {t('batch_operations_outdated_message')}
            </Alert>
          </Box>
        </Portal>
      )}
    </>
  );
};
