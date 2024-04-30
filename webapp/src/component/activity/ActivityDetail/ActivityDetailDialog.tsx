import { useMemo, useState } from 'react';
import { useTranslate, T } from '@tolgee/react';
import {
  DialogContent,
  DialogTitle,
  DialogActions,
  FormControlLabel,
  Switch,
  styled,
  Button,
  Box,
  Dialog,
  DialogProps,
} from '@mui/material';
import { OpenInNew } from '@mui/icons-material';

import { ActivityTitle } from '../ActivityTitle';
import { buildActivity } from '../activityTools';
import { ActivityModel } from '../types';
import { ActivityDetail } from './ActivityDetail';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';

const StyledDialogTitle = styled(DialogTitle)`
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 15px;
  max-width: 100%;
`;

const StyledDialogContent = styled(DialogContent)`
  min-width: 40vw;
  max-width: 1000px;
  min-height: 40vh;
`;

type Props = DialogProps & {
  data: ActivityModel;
  initialDiffEnabled: boolean;
  onClose: () => void;
};

export const ActivityDetailDialog: React.FC<Props> = ({
  data,
  initialDiffEnabled,
  ...dialogProps
}) => {
  const isBatch = !data.modifiedEntities;
  const activity = useMemo(() => buildActivity(data), [data]);
  const filteredActivity = useMemo(() => buildActivity(data, true), [data]);
  const [diffEnabled, setDiffEnabled] = useState(initialDiffEnabled);
  const project = useProject();

  const toggleDiff = () => {
    setDiffEnabled(!diffEnabled);
  };

  const { t } = useTranslate();

  return (
    <Dialog {...dialogProps}>
      <StyledDialogTitle sx={{ alignItems: 'center' }}>
        <Box>
          <ActivityTitle activity={filteredActivity} />
        </Box>
        <Box display="flex" marginLeft={2}>
          {isBatch && (
            <Button
              color="primary"
              size="medium"
              href={`${LINKS.PROJECT_TRANSLATIONS.build({
                [PARAMS.PROJECT_ID]: project.id,
              })}?revision=${data.revisionId}`}
              endIcon={<OpenInNew fontSize="small" />}
              target="_blank"
              rel="noreferrer noopener"
            >
              <T keyName="activity_detail_translation_view_link" />
            </Button>
          )}
          <FormControlLabel
            control={
              <Switch
                size="small"
                checked={diffEnabled}
                onChange={toggleDiff}
              />
            }
            label={t('dashboard_activity_differences')}
            labelPlacement="start"
          />
        </Box>
      </StyledDialogTitle>
      <StyledDialogContent>
        <ActivityDetail
          data={data}
          activity={activity}
          diffEnabled={diffEnabled}
        />
      </StyledDialogContent>
      <DialogActions>
        <Button onClick={dialogProps?.onClose}>
          <T keyName="global_close_button" />
        </Button>
      </DialogActions>
    </Dialog>
  );
};
