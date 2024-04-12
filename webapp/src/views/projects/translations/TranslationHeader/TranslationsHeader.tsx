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
import { SelectAllCheckbox } from '../BatchOperations/SelectAllCheckbox';

const StyledResultCount = styled('div')`
  padding: 9px 0px 4px 0px;
  margin-left: 15px;
  display: flex;
  align-items: center;
  gap: 13px;
`;

const StyledDialog = styled(Dialog)`
  transition: 'margin-bottom 0.2s ease-in-out';
`;

export const TranslationsHeader = () => {
  const [newCreateDialog, setNewCreateDialog] = useUrlSearchState('create', {
    defaultVal: 'false',
  });
  const { height: bottomPanelHeight } = useBottomPanel();
  const rightPanelWidth = useGlobalContext((c) => c.layout.rightPanelWidth);
  const [dirty, setDirty] = useState(false);

  const onDialogOpen = () => {
    setNewCreateDialog('true');
  };

  const onDialogClose = () => {
    setNewCreateDialog('false');
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
        onConfirm: onDialogClose,
      });
    } else {
      onDialogClose();
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
          <SelectAllCheckbox />
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
      {dataReady && newCreateDialog === 'true' && (
        <StyledDialog
          open={true}
          onClose={closeGracefully}
          fullWidth
          maxWidth="md"
          keepMounted={false}
          style={{ marginBottom: bottomPanelHeight }}
        >
          <KeyCreateDialog onClose={onDialogClose} onDirtyChange={setDirty} />
        </StyledDialog>
      )}
    </>
  );
};
