import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Radio,
  RadioGroup,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

type Props = {
  project: { projectId: number; projectName: string } | null;
  keepData: boolean;
  onKeepDataChange: (keep: boolean) => void;
  onConfirm: () => void;
  onCancel: () => void;
};

export const ConfirmRemoveProjectDialog: React.VFC<Props> = ({
  project,
  keepData,
  onKeepDataChange,
  onConfirm,
  onCancel,
}) => {
  const { t } = useTranslate();

  return (
    <Dialog open={!!project} onClose={onCancel}>
      <DialogTitle>
        <T
          keyName="tm_settings_remove_project_title"
          defaultValue="Remove {projectName}"
          params={{ projectName: project?.projectName ?? '' }}
        />
      </DialogTitle>
      <DialogContent>
        <Typography variant="body2" mb={2}>
          <T
            keyName="tm_settings_remove_project_message"
            defaultValue="This project will be disconnected from the translation memory. What should happen to the entries?"
          />
        </Typography>
        <RadioGroup
          value={keepData ? 'keep' : 'discard'}
          onChange={(e) => onKeepDataChange(e.target.value === 'keep')}
        >
          <FormControlLabel
            value="discard"
            control={<Radio />}
            label={t(
              'tm_settings_disconnect_discard',
              'Just disconnect — entries stay in the shared memory'
            )}
          />
          <FormControlLabel
            value="keep"
            control={<Radio />}
            label={t(
              'tm_settings_disconnect_keep',
              "Copy entries into the project's own memory before disconnecting"
            )}
          />
        </RadioGroup>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel}>
          <T keyName="global_cancel_button" defaultValue="Cancel" />
        </Button>
        <Button variant="contained" color="primary" onClick={onConfirm}>
          <T keyName="tm_settings_remove_confirm" defaultValue="Remove" />
        </Button>
      </DialogActions>
    </Dialog>
  );
};
