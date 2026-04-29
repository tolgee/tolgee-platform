import React, { useState } from 'react';
import {
  Box,
  Dialog,
  DialogContent,
  DialogTitle,
  Switch,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { DialogCancelSaveActions } from 'tg.ee.module/translationMemory/components/DialogCancelSaveActions';

type AssignmentModel =
  components['schemas']['ProjectTranslationMemoryAssignmentModel'];

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
  projectId: number;
  assignment: AssignmentModel;
};

export const ProjectTmAssignmentDialog: React.VFC<Props> = ({
  open,
  onClose,
  onFinished,
  projectId,
  assignment,
}) => {
  const { t } = useTranslate();
  const [readAccess, setReadAccess] = useState(assignment.readAccess);
  const [writeAccess, setWriteAccess] = useState(assignment.writeAccess);

  const updateMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translation-memories/{translationMemoryId}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/translation-memories',
  });

  const handleSave = () => {
    updateMutation.mutate(
      {
        path: {
          projectId,
          translationMemoryId: assignment.translationMemoryId,
        },
        content: {
          'application/json': {
            readAccess,
            writeAccess,
            priority: assignment.priority,
          },
        },
      },
      { onSuccess: onFinished }
    );
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{assignment.translationMemoryName}</DialogTitle>
      <DialogContent>
        <Box display="flex" alignItems="center" justifyContent="space-between">
          <Typography variant="body2">
            {t('project_tm_assignment_read', 'Read access')}
          </Typography>
          <Switch checked={readAccess} onChange={(_, v) => setReadAccess(v)} />
        </Box>
        <Typography variant="caption" color="text.secondary" mb={2}>
          <T
            keyName="project_tm_assignment_read_hint"
            defaultValue="TM suggestions are drawn from this memory when enabled."
          />
        </Typography>

        <Box
          display="flex"
          alignItems="center"
          justifyContent="space-between"
          mt={2}
        >
          <Typography variant="body2">
            {t('project_tm_assignment_write', 'Write access')}
          </Typography>
          <Switch
            checked={writeAccess}
            onChange={(_, v) => setWriteAccess(v)}
          />
        </Box>
        <Typography variant="caption" color="text.secondary">
          <T
            keyName="project_tm_assignment_write_hint"
            defaultValue="New translations in this project are written to this memory when enabled."
          />
        </Typography>
      </DialogContent>
      <DialogCancelSaveActions
        onCancel={onClose}
        onSave={handleSave}
        saving={updateMutation.isLoading}
      />
    </Dialog>
  );
};
