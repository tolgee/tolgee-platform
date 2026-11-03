import { useState } from 'react';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import {
  InContextValue,
  isInContextValueValid,
  inContextValueToSave,
} from 'tg.views/projects/project/components/communityAppUrl';
import { CommunityInContextContent } from 'tg.views/projects/project/components/CommunityInContextContent';
import { useCommunitySetupTexts } from 'tg.views/projects/project/components/useCommunitySetupTexts';

type Props = {
  value: InContextValue;
  saving: boolean;
  onSave: (value: InContextValue) => void;
};

export const CommunityInContextDialog = ({ value, saving, onSave }: Props) => {
  const { t } = useTranslate();
  const stepTexts = useCommunitySetupTexts();
  const [draft, setDraft] = useState(value);
  const canClose = isInContextValueValid(draft) && !saving;
  const saveAndClose = () => canClose && onSave(inContextValueToSave(draft));

  return (
    <Dialog
      open
      onClose={saveAndClose}
      fullWidth
      maxWidth="sm"
      data-cy="community-in-context-dialog"
    >
      <DialogTitle sx={{ pb: 1 }}>
        {t('community_settings_in_context', 'In-context translation')}
      </DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" mb={2}>
          {stepTexts.inContext.hint}
        </Typography>
        <CommunityInContextContent value={draft} onChange={setDraft} />
      </DialogContent>
      <DialogActions>
        <Button
          onClick={saveAndClose}
          disabled={!canClose}
          data-cy="community-in-context-close"
        >
          {t('global_close_button', 'Close')}
        </Button>
      </DialogActions>
    </Dialog>
  );
};
