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

import { CommunityShareContent } from 'tg.views/projects/project/components/CommunityShareContent';
import { useCommunitySetupTexts } from 'tg.views/projects/project/components/useCommunitySetupTexts';

type Props = {
  projectId: number;
  shared: boolean;
  saving: boolean;
  onSave: (shared: boolean) => void;
};

export const CommunityShareDialog = ({
  projectId,
  shared,
  saving,
  onSave,
}: Props) => {
  const { t } = useTranslate();
  const stepTexts = useCommunitySetupTexts();
  const [draft, setDraft] = useState(shared);
  const saveAndClose = () => !saving && onSave(draft);

  return (
    <Dialog
      open
      onClose={saveAndClose}
      fullWidth
      maxWidth="sm"
      data-cy="community-share-dialog"
    >
      <DialogTitle sx={{ pb: 1 }}>
        {t('community_settings_share', 'Share it')}
      </DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" mb={2}>
          {stepTexts.share.hint}
        </Typography>
        <CommunityShareContent
          projectId={projectId}
          shared={draft}
          onSharedChange={setDraft}
        />
      </DialogContent>
      <DialogActions>
        <Button
          onClick={saveAndClose}
          disabled={saving}
          data-cy="community-share-close"
        >
          {t('global_close_button', 'Close')}
        </Button>
      </DialogActions>
    </Dialog>
  );
};
