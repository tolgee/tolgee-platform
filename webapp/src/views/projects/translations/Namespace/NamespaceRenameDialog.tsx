import {
  Dialog,
  DialogTitle,
  DialogContent,
  TextField,
  DialogActions,
  Button,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useState } from 'react';

type Props = {
  namespace: string;
  onClose: () => void;
};

export const NamespaceRenameDialog: React.FC<Props> = ({
  namespace,
  onClose,
}) => {
  const t = useTranslate();
  const [newName, setNewName] = useState(namespace);

  const handleConfirm = () => {
    // missing endpoint for renaming
  };

  return (
    <Dialog open onClose={onClose} fullWidth>
      <DialogTitle>{t('namespace_rename_title')}</DialogTitle>

      <DialogContent>
        <TextField
          data-cy="namespaces-rename-text-field"
          onChange={(e) => {
            setNewName(e.target.value);
          }}
          placeholder={t('namespace_rename_placeholder')}
          value={newName}
          fullWidth
          size="small"
          autoFocus
          onKeyPress={(e) => {
            if (e.key === 'Enter') {
              handleConfirm();
            }
          }}
        />
      </DialogContent>
      <DialogActions>
        <Button data-cy="global-confirmation-cancel" onClick={onClose}>
          {t('namespace_rename_cancel')}
        </Button>
        <Button
          data-cy="global-confirmation-confirm"
          color="primary"
          onClick={handleConfirm}
        >
          {t('namespace_rename_confirm')}
        </Button>
      </DialogActions>
    </Dialog>
  );
};
