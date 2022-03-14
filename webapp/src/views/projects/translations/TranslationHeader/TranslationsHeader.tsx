import {
  Typography,
  Dialog,
  makeStyles,
  useMediaQuery,
} from '@material-ui/core';
import { T } from '@tolgee/react';

import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useBottomPanel } from 'tg.component/bottomPanel/BottomPanelContext';

import { useTranslationsSelector } from '../context/TranslationsContext';
import { KeyCreateDialog } from './KeyCreateDialog';
import { TranslationControls } from './TranslationControls';
import { TranslationControlsCompact } from './TranslationControlsCompact';

const useStyles = makeStyles((theme) => ({
  resultCount: {
    marginLeft: 1,
    marginTop: theme.spacing(),
  },
  modal: {
    transition: 'margin-bottom 0.2s',
  },
}));

export const TranslationsHeader = () => {
  const classes = useStyles();
  const [newDialog, setNewDialog] = useUrlSearchState('create', {
    defaultVal: 'false',
  });
  const { height: bottomPanelHeight } = useBottomPanel();

  const onDialogOpen = () => {
    setNewDialog('true');
  };

  const isSmall = useMediaQuery('@media (max-width: 1000px)');

  const translationsTotal = useTranslationsSelector((c) => c.translationsTotal);

  const dataReady = useTranslationsSelector((c) => c.dataReady);

  return (
    <>
      {isSmall ? (
        <TranslationControlsCompact onDialogOpen={onDialogOpen} />
      ) : (
        <TranslationControls onDialogOpen={onDialogOpen} />
      )}
      {dataReady && translationsTotal ? (
        <div className={classes.resultCount}>
          <Typography
            color="textSecondary"
            variant="body2"
            data-cy="translations-key-count"
          >
            <T parameters={{ count: String(translationsTotal) }}>
              translations_results_count
            </T>
          </Typography>
        </div>
      ) : null}
      {dataReady && newDialog === 'true' && (
        <Dialog
          open={true}
          onClose={() => setNewDialog('false')}
          fullWidth
          maxWidth="md"
          keepMounted={false}
          className={classes.modal}
          style={{ marginBottom: bottomPanelHeight }}
        >
          <KeyCreateDialog onClose={() => setNewDialog('false')} />
        </Dialog>
      )}
    </>
  );
};
