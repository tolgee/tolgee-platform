import {
  Box,
  Button,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Switch,
  styled,
} from '@mui/material';
import { useMemo } from 'react';
import { Share03 } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';

import { ActivityDetail } from './ActivityDetail';
import { ActivityTitle } from '../ActivityTitle';
import { ActivityModel } from '../types';
import { buildActivity } from '../activityTools';

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

type Props = {
  onClose: () => void;
  data: ActivityModel;
  onToggleDiff: () => void;
  diffEnabled: boolean;
};

export const ActivityDetailContent = ({
  onClose,
  data,
  onToggleDiff,
  diffEnabled,
}: Props) => {
  const { t } = useTranslate();
  const project = useProject();

  const activity = useMemo(() => buildActivity(data), [data]);
  const filteredActivity = useMemo(() => buildActivity(data, true), [data]);

  const isBatch = !data?.modifiedEntities;

  return (
    <>
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
              })}?activity=${data.revisionId}`}
              endIcon={<Share03 width={18} />}
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
                onChange={onToggleDiff}
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
        <Button onClick={onClose}>
          <T keyName="global_close_button" />
        </Button>
      </DialogActions>
    </>
  );
};
