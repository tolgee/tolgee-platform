import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
} from '@mui/material';
import { T } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type LanguageModel = components['schemas']['LanguageModel'];

type Props = {
  language: LanguageModel | true;
  onClose: () => void;
};

export const EmptyScopeDialog = ({ language, onClose }: Props) => {
  return (
    <Dialog open={true} onClose={onClose} data-cy="empty-scope-dialog">
      <DialogTitle>
        {typeof language === 'object' && language.name ? (
          <T
            keyName="create_task_empty_scope_dialog_message_language"
            params={{ language: language.name }}
          />
        ) : (
          <T keyName="create_task_empty_scope_dialog_message" />
        )}
      </DialogTitle>
      <DialogContent>
        <T keyName="create_task_empty_scope_dialog_explanation" />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="primary">
          <T keyName="create_task_empty_scope_dialog_ok" />
        </Button>
      </DialogActions>
    </Dialog>
  );
};
