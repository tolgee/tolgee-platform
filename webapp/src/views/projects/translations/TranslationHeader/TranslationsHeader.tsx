import { Typography, Dialog, useMediaQuery, styled } from '@mui/material';
import { T } from '@tolgee/react';

import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useBottomPanel } from 'tg.component/bottomPanel/BottomPanelContext';

import { useTranslationsSelector } from '../context/TranslationsContext';
import { KeyCreateDialog } from './KeyCreateDialog';
import { TranslationControls } from './TranslationControls';
import { TranslationControlsCompact } from './TranslationControlsCompact';

const StyledResultCount = styled('div')`
  margin-left: 1px;
  margin-top: ${({ theme }) => theme.spacing(1)};
`;

const StyledDialog = styled(Dialog)`
  transition: 'margin-bottom 0.2s ease-in-out';
`;

export const TranslationsHeader = () => {
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
        <StyledResultCount>
          <Typography
            color="textSecondary"
            variant="body2"
            data-cy="translations-key-count"
          >
            <T params={{ count: String(translationsTotal) }}>
              translations_results_count
            </T>
          </Typography>
        </StyledResultCount>
      ) : null}
      {dataReady && newDialog === 'true' && (
        <StyledDialog
          open={true}
          onClose={() => setNewDialog('false')}
          fullWidth
          maxWidth="md"
          keepMounted={false}
          style={{ marginBottom: bottomPanelHeight }}
        >
          <KeyCreateDialog onClose={() => setNewDialog('false')} />
        </StyledDialog>
      )}
    </>
  );
};
