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
} from '@mui/material';
import { Dialog, DialogProps } from '@mui/material';

import { ActivityTitle } from '../ActivityTitle';
import { buildActivity } from '../activityTools';
import { ActivityModel } from '../types';
import { ActivityDetail } from './ActivityDetail';

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
  const activity = useMemo(() => buildActivity(data), [data]);
  const filteredActivity = useMemo(() => buildActivity(data, true), [data]);
  const [diffEnabled, setDiffEnabled] = useState(initialDiffEnabled);

  const toggleDiff = () => {
    setDiffEnabled(!diffEnabled);
  };

  const { t } = useTranslate();

  return (
    <Dialog {...dialogProps}>
      <StyledDialogTitle>
        <span>
          <ActivityTitle activity={filteredActivity} />
        </span>
        <FormControlLabel
          control={
            <Switch size="small" checked={diffEnabled} onChange={toggleDiff} />
          }
          label={t('dashboard_activity_differences')}
          labelPlacement="start"
        />
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
