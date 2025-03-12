import { Checkbox, FormControlLabel, styled, SxProps } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { LabelHint } from 'tg.component/common/LabelHint';
import { QUERY } from 'tg.constants/links';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';

const StyledLabel = styled('div')`
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  flex-shrink: 1;
`;

type Props = {
  sx?: SxProps;
};

export const PrefilterTaskHideDoneSwitch = ({ sx }: Props) => {
  const [taskHideDone, setTaskHideDone] = useUrlSearchState(
    QUERY.TRANSLATIONS_PREFILTERS_TASK_HIDE_CLOSED
  );
  const { t } = useTranslate();

  return (
    <FormControlLabel
      {...{ sx }}
      label={
        <LabelHint title={t('task_filter_hide_done_hint')}>
          <StyledLabel>{t('task_filter_hide_done')}</StyledLabel>
        </LabelHint>
      }
      control={
        <Checkbox
          size="small"
          checked={taskHideDone === 'true'}
          onChange={() =>
            setTaskHideDone(taskHideDone === 'true' ? undefined : 'true')
          }
        />
      }
    />
  );
};
