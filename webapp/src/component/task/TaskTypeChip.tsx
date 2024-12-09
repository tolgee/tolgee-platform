import { Chip, styled, Theme, useTheme } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { useTaskTypeTranslation } from 'tg.translationTools/useTaskTranslation';

type TaskType = components['schemas']['TaskModel']['type'];

const StyledChip = styled(Chip)``;

export function getBackgroundColor(type: TaskType, theme: Theme) {
  switch (type) {
    case 'TRANSLATE':
      return theme.palette.tokens.text._states.focus;
    case 'REVIEW':
      return theme.palette.tokens.secondary._states.focus;
  }
}

type Props = {
  type: TaskType;
};

export const TaskTypeChip = ({ type }: Props) => {
  const translateTaskType = useTaskTypeTranslation();
  const theme = useTheme();

  return (
    <StyledChip
      size="small"
      label={translateTaskType(type)}
      sx={{ background: getBackgroundColor(type, theme) }}
    />
  );
};
