import { AlertTriangle } from '@untitled-ui/icons-react';
import { Box, Skeleton, styled, Tooltip, useTheme } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { useNumberFormatter } from 'tg.hooks/useLocale';
import { User } from 'tg.component/UserAccount';
import { AssigneeSearchSelect } from '../assigneeSelect/AssigneeSearchSelect';
import { useTaskTypeTranslation } from 'tg.translationTools/useTaskTranslation';

type TaskType = components['schemas']['TaskModel']['type'];
type LanguageModel = components['schemas']['LanguageModel'];
type KeysScopeView = components['schemas']['KeysScopeView'];

const StyledContainer = styled('div')`
  display: grid;
  padding: 16px 20px;
  border-radius: 8px;
  background: ${({ theme }) => theme.palette.tokens.background.selected};
  ${({ theme }) => theme.breakpoints.down('sm')} {
    grid-template-columns: 1fr;
  }
`;

const StyledContent = styled('div')`
  display: flex;
  align-items: center;
  padding: 8px 12px;
  flex-shrink: 0;
  gap: 8px;
`;

const StyledMetric = styled('div')`
  font-size: 15px;
  color: ${({ theme }) => theme.palette.tokens.text.secondary};
`;

const StyledSmallCaption = styled('div')`
  font-size: 12px;
  position: relative;
  margin-bottom: -3px;
`;

type Props = {
  type: TaskType;
  language: LanguageModel;
  assignees: User[];
  onUpdateAssignees: (users: User[]) => void;
  projectId: number;
  hideAssignees?: boolean;
  scope: KeysScopeView | undefined;
};

export const TaskPreview = ({
  type,
  language,
  assignees,
  onUpdateAssignees,
  projectId,
  hideAssignees,
  scope,
}: Props) => {
  const { t } = useTranslate();
  const formatNumber = useNumberFormatter();
  const theme = useTheme();
  const translateTaskType = useTaskTypeTranslation();

  return (
    <StyledContainer
      data-cy="task-preview"
      sx={{
        gridTemplateColumns: hideAssignees ? '1fr 3fr' : '1fr 3fr 2fr',
      }}
    >
      <StyledContent>
        <FlagImage flagEmoji={language.flagEmoji!} height={20} />
        <Box
          sx={{ fontWeight: language.base ? 'bold' : 'normal' }}
          data-cy="task-preview-language"
        >
          {language.name}
        </Box>
      </StyledContent>

      <Box display="flex" gap="60px" justifyContent="center">
        <Box display="grid" alignContent="center">
          <StyledMetric>{t('create_task_preview_keys')}</StyledMetric>
          <StyledMetric data-cy="task-preview-keys">
            {scope ? (
              <Box display="flex" alignItems="center">
                {formatNumber(scope.keyCount)}
                {scope.keyCount !== scope.keyCountIncludingConflicts && (
                  <Tooltip
                    title={t('create_task_preview_missing_keys_hint', {
                      type: translateTaskType(type).toLocaleLowerCase(),
                    })}
                  >
                    <Box
                      display="flex"
                      alignItems="center"
                      data-cy="task-preview-alert"
                    >
                      <AlertTriangle
                        height={15}
                        color={theme.palette.tokens.warning.main}
                      />
                    </Box>
                  </Tooltip>
                )}
              </Box>
            ) : (
              <Skeleton />
            )}
          </StyledMetric>
        </Box>
        <Box display="grid" alignContent="center">
          <StyledMetric>{t('create_task_preview_words')}</StyledMetric>
          <StyledMetric data-cy="task-preview-words">
            {scope ? formatNumber(scope.wordCount) : <Skeleton />}
          </StyledMetric>
        </Box>
        <Box display="grid" alignContent="center">
          <StyledMetric>{t('create_task_preview_characters')}</StyledMetric>
          <StyledMetric data-cy="task-preview-characters">
            {scope ? formatNumber(scope.characterCount) : <Skeleton />}
          </StyledMetric>
        </Box>
      </Box>
      {!hideAssignees && (
        <AssigneeSearchSelect
          value={assignees}
          projectId={projectId}
          onChange={onUpdateAssignees}
          sx={{
            alignSelf: 'center',
          }}
          label={
            <StyledSmallCaption>
              {t('create_task_preview_assignee')}
            </StyledSmallCaption>
          }
          filters={{
            filterMinimalScope: 'TRANSLATIONS_VIEW',
            filterViewLanguageId: language.id,
          }}
        />
      )}
    </StyledContainer>
  );
};
