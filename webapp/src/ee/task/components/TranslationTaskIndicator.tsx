import { useProject } from 'tg.hooks/useProject';
import { useTheme } from '@mui/material';
import clsx from 'clsx';
import { StyledImgWrapper } from 'tg.component/TranslationFlagIcon';
import { ClipboardCheck } from '@untitled-ui/icons-react';
import { TaskTooltip } from './TaskTooltip';
import { StyledTranslationFlagsContainer } from 'tg.views/projects/translations/cell/TranslationFlags';
import { TranslationTaskIndicatorProps } from '../../../eeSetup/EeModuleType';

export const TranslationTaskIndicator: React.FC<
  TranslationTaskIndicatorProps
> = ({ task }) => {
  const project = useProject();
  const theme = useTheme();

  return (
    <>
      {task && (
        <TaskTooltip taskNumber={task.number} project={project}>
          <StyledTranslationFlagsContainer
            className={clsx({ clickDisabled: true })}
            data-cy="translations-task-indicator"
          >
            <StyledImgWrapper>
              <ClipboardCheck color={theme.palette.text.primary} />
            </StyledImgWrapper>
          </StyledTranslationFlagsContainer>
        </TaskTooltip>
      )}
    </>
  );
};
