import { Chip, styled, useTheme } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { useTaskTranslation } from 'tg.translationTools/useTaskTranslation';

type TaskType = components['schemas']['TaskModel']['type'];

const StyledChip = styled(Chip)``;

type Props = {
  type: TaskType;
};

export const TaskTypeChip = ({ type }: Props) => {
  const translateTaskType = useTaskTranslation();
  const theme = useTheme();

  function getBackgroundColor() {
    switch (type) {
      case 'TRANSLATE':
        return theme.palette.tokens.text._states.focus;
      case 'REVIEW':
        return theme.palette.tokens.secondary._states.focus;
    }
  }

  return (
    <StyledChip
      size="small"
      label={translateTaskType(type)}
      sx={{ background: getBackgroundColor() }}
    />
  );
};
