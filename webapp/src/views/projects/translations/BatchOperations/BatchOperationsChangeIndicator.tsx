import { Alert, Box, Portal } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useEffect, useState } from 'react';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useProjectContext } from 'tg.hooks/ProjectContext';
import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import { BatchJobStatus } from './types';

const NOTIFY_STATUSES: BatchJobStatus[] = ['SUCCESS', 'CANCELLED'];

export const BatchOperationsChangeIndicator = () => {
  const { t } = useTranslate();
  const { refetchTranslations } = useTranslationsActions();
  const [isRefetching, setIsRefetching] = useState(false);
  const lastJob = useProjectContext((c) => {
    return c.batchOperations
      ? c.batchOperations.find((o) => NOTIFY_STATUSES.includes(o.status))?.id
      : 'not-loaded';
  });
  const isFetching = useTranslationsSelector((c) => c.isFetching);

  const [previousLast, setPreviousLast] = useState(lastJob);
  const [dataChanged, setDataChanged] = useState(false);

  async function handleRefetch() {
    setIsRefetching(true);
    await refetchTranslations();
    setIsRefetching(false);
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
  }, [isFetching]);

  return (
    <>
      {(dataChanged || isRefetching) && (
        <Portal>
          <Box sx={{ zIndex: 10000, position: 'fixed', right: 10, top: 60 }}>
            <Alert
              elevation={1}
              severity="warning"
              action={
                <LoadingButton
                  color="inherit"
                  size="small"
                  onClick={handleRefetch}
                  loading={isRefetching}
                >
                  {t('batch_operations_refresh_button')}
                </LoadingButton>
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
