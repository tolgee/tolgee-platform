import { Typography, Dialog, useMediaQuery, styled } from '@mui/material';
import { T } from '@tolgee/react';

import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useBottomPanel } from 'tg.component/bottomPanel/BottomPanelContext';

import { useTranslationsSelector } from '../context/TranslationsContext';
import { KeyCreateDialog } from './KeyCreateDialog';
import { TranslationControls } from './TranslationControls';
import { TranslationControlsCompact } from './TranslationControlsCompact';
import { useState } from 'react';
import { confirmation } from 'tg.hooks/confirmation';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

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
  const rightPanelWidth = useGlobalContext((c) => c.rightPanelWidth);
  const [dirty, setDirty] = useState(false);

  const onDialogOpen = () => {
    setNewDialog('true');
  };

  const isSmall = useMediaQuery(
    `@media(max-width: ${rightPanelWidth + 1000}px)`
  );

  const translationsTotal = useTranslationsSelector((c) => c.translationsTotal);

  const dataReady = useTranslationsSelector((c) => c.dataReady);

  function closeGracefully() {
    if (dirty) {
      confirmation({
        message: <T keyName="translations_new_key_discard_message" />,
        confirmButtonText: <T keyName="translations_new_key_discard_button" />,
        onConfirm: () => setNewDialog('false'),
      });
    } else {
      setNewDialog('false');
    }
  }

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
            <T
              keyName="translations_results_count"
              params={{ count: String(translationsTotal) }}
            />
          </Typography>
        </StyledResultCount>
      ) : null}
      {dataReady && newDialog === 'true' && (
        <StyledDialog
          open={true}
          onClose={closeGracefully}
          fullWidth
          maxWidth="md"
          keepMounted={false}
          style={{ marginBottom: bottomPanelHeight }}
        >
          <KeyCreateDialog
            onClose={() => setNewDialog('false')}
            onDirtyChange={setDirty}
          />
        </StyledDialog>
      )}
    </>
  );
};
