import { Box, MenuItem, Paper, styled, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { Select } from 'tg.component/common/Select';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import {
  AiReviewSettingsModel,
  SourceChangeAction,
} from 'tg.ee.module/aiReview/utils/types';

const StyledList = styled(Paper)`
  display: grid;
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
`;

const StyledRow = styled('div')`
  display: grid;
  grid-template-columns: 1fr 260px;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(2)};
  padding: ${({ theme }) => theme.spacing(1.5, 2)};

  & + & {
    border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  }

  ${({ theme }) => theme.breakpoints.down('sm')} {
    grid-template-columns: 1fr;
  }
`;

type SettingName = keyof AiReviewSettingsModel;

type Props = {
  projectId: number;
  canEdit: boolean;
};

export const AiReviewSourceChangeSettings = ({ projectId, canEdit }: Props) => {
  const { t } = useTranslate();

  const settingsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/ai-review/settings',
    method: 'get',
    path: { projectId },
  });

  const updateLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/ai-review/settings',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/ai-review/settings',
  });

  const settings = settingsLoadable.data;
  if (!settings) {
    return null;
  }

  const rows: { name: SettingName; label: string; hint: string }[] = [
    {
      name: 'onSourceChangeAiApproved',
      label: t(
        'ai_review_source_change_ai_approved',
        'Machine translations approved by AI'
      ),
      hint: t(
        'ai_review_source_change_ai_approved_hint',
        'No human has touched them, so they behave like other machine translations.'
      ),
    },
    {
      name: 'onSourceChangeHumanEdited',
      label: t(
        'ai_review_source_change_human_edited',
        'Translations edited by a human'
      ),
      hint: t(
        'ai_review_source_change_human_edited_hint',
        'Written or changed by a person, not reviewed yet.'
      ),
    },
    {
      name: 'onSourceChangeHumanReviewed',
      label: t(
        'ai_review_source_change_human_reviewed',
        'Translations reviewed by a human'
      ),
      hint: t(
        'ai_review_source_change_human_reviewed_hint',
        'Marked as reviewed by a person.'
      ),
    },
  ];

  function update(name: SettingName, value: SourceChangeAction) {
    updateLoadable.mutate(
      {
        path: { projectId },
        content: { 'application/json': { ...settings!, [name]: value } },
      },
      {
        onSuccess() {
          messageService.success(
            <T
              keyName="ai_review_source_change_saved"
              defaultValue="Settings saved"
            />
          );
        },
      }
    );
  }

  return (
    <Box display="grid" gap={1.5} data-cy="ai-review-source-change-settings">
      <Box display="grid" gap={0.5}>
        <Typography variant="h6">
          {t('ai_review_source_change_title', 'When the source text changes')}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {t(
            'ai_review_source_change_description',
            'Choose what happens to existing translations when someone edits the source text. Translating again uses machine translation from the auto translation settings of the language.'
          )}
        </Typography>
      </Box>
      <StyledList elevation={0}>
        {rows.map((row) => (
          <StyledRow key={row.name}>
            <Box display="grid">
              <Typography variant="body1">{row.label}</Typography>
              <Typography variant="body2" color="text.secondary">
                {row.hint}
              </Typography>
            </Box>
            <Select
              value={settings[row.name]}
              onChange={(e) =>
                update(row.name, e.target.value as SourceChangeAction)
              }
              disabled={!canEdit || updateLoadable.isLoading}
              minHeight={false}
              data-cy="ai-review-source-change-select"
              data-cy-setting={row.name}
            >
              <MenuItem
                value="RETRANSLATE"
                data-cy="ai-review-source-change-option"
                data-cy-action="RETRANSLATE"
              >
                {t('ai_review_source_change_retranslate', 'Translate again')}
              </MenuItem>
              <MenuItem
                value="KEEP"
                data-cy="ai-review-source-change-option"
                data-cy-action="KEEP"
              >
                {t('ai_review_source_change_keep', 'Keep, mark as outdated')}
              </MenuItem>
            </Select>
          </StyledRow>
        ))}
      </StyledList>
    </Box>
  );
};
