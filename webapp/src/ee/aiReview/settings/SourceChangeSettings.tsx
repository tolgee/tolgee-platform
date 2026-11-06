import { Box, MenuItem, Select, Typography, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useEnabledFeatures } from 'tg.globalContext/helpers';

type SourceChangeSettingsModel =
  components['schemas']['SourceChangeSettingsModel'];
type SourceChangeAction = SourceChangeSettingsModel['humanEdited'];
type Field = 'aiApproved' | 'humanEdited' | 'humanReviewed';

const StyledRows = styled(Box)`
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-radius: 8px;
`;

const StyledRow = styled(Box)`
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(3)};
  padding: ${({ theme }) => theme.spacing(1.5, 2)};

  & + & {
    border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  }
`;

const StyledLockedHint = styled(Typography)`
  color: ${({ theme }) => theme.palette.text.secondary};
`;

export const SourceChangeSettings = () => {
  const { t } = useTranslate();
  const project = useProject();
  const { satisfiesPermission } = useProjectPermissions();
  const canEdit = satisfiesPermission('project.edit');
  const aiReviewEnabled = useEnabledFeatures().isEnabled('AI_REVIEW');

  const settingsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/source-change-settings',
    method: 'get',
    path: { projectId: project.id },
  });

  const updateMutation = useApiMutation({
    url: '/v2/projects/{projectId}/source-change-settings',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/source-change-settings',
  });

  const settings = settingsLoadable.data;
  if (!settings) {
    return null;
  }

  function handleChange(field: Field, action: SourceChangeAction) {
    if (!settings) {
      return;
    }
    updateMutation.mutate({
      path: { projectId: project.id },
      content: {
        'application/json': {
          aiApproved: settings.aiApproved,
          humanEdited: settings.humanEdited,
          humanReviewed: settings.humanReviewed,
          [field]: action,
        },
      },
    });
  }

  type Row = {
    field: Field;
    label: string;
    hint: string;
    locked: boolean;
  };

  const aiApprovedRow: Row[] = aiReviewEnabled
    ? [
        {
          field: 'aiApproved',
          label: t(
            'source_change_ai_approved_label',
            'Machine translations approved by AI'
          ),
          hint: t(
            'source_change_ai_approved_hint',
            'No human has touched them, so they behave like other machine translations.'
          ),
          locked: settings.aiApprovedLocked,
        },
      ]
    : [];

  const rows: Row[] = [
    ...aiApprovedRow,
    {
      field: 'humanEdited',
      label: t(
        'source_change_human_edited_label',
        'Translations edited by a human'
      ),
      hint: t(
        'source_change_human_edited_hint',
        'Written or changed by a person, not reviewed yet.'
      ),
      locked: false,
    },
    {
      field: 'humanReviewed',
      label: t(
        'source_change_human_reviewed_label',
        'Translations reviewed by a human'
      ),
      hint: t(
        'source_change_human_reviewed_hint',
        'Marked as reviewed by a person.'
      ),
      locked: settings.humanReviewedLocked,
    },
  ];

  return (
    <Box
      display="grid"
      gap={1.5}
      marginTop={4}
      data-cy="ai-review-source-change-settings"
    >
      <Typography variant="h6">
        {t('source_change_settings_title', 'When the source text changes')}
      </Typography>
      <Typography color="textSecondary">
        {t(
          'source_change_settings_description',
          'Choose what happens to existing translations when someone edits the source text. Translating again uses machine translation from the auto translation settings of the language.'
        )}
      </Typography>
      <StyledRows>
        {rows.map((row) => (
          <StyledRow key={row.field}>
            <Box display="grid" gap={0.25}>
              <Typography>{row.label}</Typography>
              <Typography variant="body2" color="textSecondary">
                {row.hint}
              </Typography>
            </Box>
            <Box display="grid" gap={0.5} justifyItems="start">
              <Select
                size="small"
                value={settings[row.field]}
                disabled={!canEdit || row.locked || updateMutation.isLoading}
                onChange={(e) =>
                  handleChange(row.field, e.target.value as SourceChangeAction)
                }
                SelectDisplayProps={
                  {
                    'data-cy': 'source-change-select',
                    'data-cy-field': row.field,
                    'data-cy-selected': settings[row.field],
                  } as any
                }
              >
                <MenuItem
                  value="RETRANSLATE"
                  data-cy="source-change-select-item"
                  data-cy-action="RETRANSLATE"
                >
                  {t('source_change_action_retranslate', 'Translate again')}
                </MenuItem>
                <MenuItem
                  value="MARK_OUTDATED"
                  data-cy="source-change-select-item"
                  data-cy-action="MARK_OUTDATED"
                >
                  {t(
                    'source_change_action_mark_outdated',
                    'Keep, mark as outdated'
                  )}
                </MenuItem>
              </Select>
              {row.locked && (
                <StyledLockedHint
                  variant="caption"
                  data-cy="source-change-locked-hint"
                  data-cy-field={row.field}
                >
                  {t(
                    'source_change_locked_hint',
                    'Cannot be changed because the project uses Translation protection.'
                  )}
                </StyledLockedHint>
              )}
            </Box>
          </StyledRow>
        ))}
      </StyledRows>
    </Box>
  );
};
