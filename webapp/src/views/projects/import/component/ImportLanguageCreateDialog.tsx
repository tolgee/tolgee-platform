import { FunctionComponent } from 'react';
import { Box, Dialog, DialogContent, DialogTitle } from '@mui/material';
import { T } from '@tolgee/react';

import { CreateSingleLanguage } from 'tg.component/languages/CreateSingleLanguage';

export const ImportLanguageCreateDialog: FunctionComponent<{
  open: boolean;
  onCreated: (id: number) => void;
  onClose: () => void;
}> = (props) => {
  return (
    <Dialog
      open={props.open}
      aria-labelledby="form-dialog-title"
      onClose={() => props.onClose()}
      maxWidth={'md'}
    >
      <DialogTitle id="form-dialog-title">
        <T>import_add_new_language_dialog_title</T>
      </DialogTitle>
      <DialogContent>
        <Box minWidth={600}>
          <CreateSingleLanguage
            onCreated={(language) => {
              props.onCreated(language.id);
            }}
            onCancel={props.onClose}
          />
        </Box>
      </DialogContent>
    </Dialog>
  );
};
