import { Typography, Dialog, useMediaQuery, styled, Box } from '@mui/material';
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
import { StickyHeader } from './StickyHeader';
import { Prefilter } from '../prefilters/Prefilter';

const StyledResultCount = styled('div')`
  padding: 0px 0px 4px 0px;
  margin-left: 15px;
  display: flex;
  align-items: center;
  gap: 13px;
`;

const StyledDialog = styled(Dialog)`
  transition: 'margin-bottom 0.2s ease-in-out';
`;

export const TranslationsHeader = () => {
  const prefilter = useTranslationsSelector((c) => c.prefilter);
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
    `@media(max-width: ${rightPanelWidth + 1200}px)`
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

  const controls = isSmall ? (
    <Box sx={{ padding: '4px 0px' }}>
      <TranslationControlsCompact onDialogOpen={onDialogOpen} />
    </Box>
  ) : (
    <Box sx={{ padding: '8px 0px' }}>
      <TranslationControls onDialogOpen={onDialogOpen} />
    </Box>
  );

  return (
    <>
      {prefilter && (
        <>
          <StickyHeader height={48}>
            <Box sx={{ paddingTop: '4px', paddingX: 0.5 }}>
              <Prefilter prefilter={prefilter} />
            </Box>
          </StickyHeader>
        </>
      )}

      {!prefilter ? (
        <StickyHeader height={isSmall ? 46 : 55}>{controls}</StickyHeader>
      ) : (
        <Box sx={{ marginX: -0.5 }}>{controls}</Box>
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
