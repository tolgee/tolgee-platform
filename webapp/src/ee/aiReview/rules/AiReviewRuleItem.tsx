import { useRef, useState } from 'react';
import {
  Box,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  Paper,
  Switch,
  Typography,
  styled,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { DotsVertical, Edit02 } from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';
import { confirmation } from 'tg.hooks/confirmation';
import { messageService } from 'tg.service/MessageService';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { AiReviewRuleScopeChips } from './AiReviewRuleScopeChips';
import { AiReviewRuleDialog } from './dialog/AiReviewRuleDialog';
import { useRuleLabel } from './useRuleLabel';

type AiReviewRuleModel = components['schemas']['AiReviewRuleModel'];

const StyledItem = styled(Paper)`
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  padding: 16px;
`;

const StyledInfoLine = styled(Typography)`
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 14px;
`;

type Props = {
  rule: AiReviewRuleModel;
  promptNames: Record<number, string>;
};

export const AiReviewRuleItem = ({ rule, promptNames }: Props) => {
  const { t } = useTranslate();
  const project = useProject();
  const { satisfiesPermission } = useProjectPermissions();
  const canEdit = satisfiesPermission('prompts.edit');
  const ruleLabel = useRuleLabel();
  const label = ruleLabel(rule);
  const [menuOpen, setMenuOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const menuButtonRef = useRef<HTMLButtonElement>(null);

  const enabledLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/ai-review-rules/{ruleId}/enabled',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/ai-review-rules',
  });

  const deleteLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/ai-review-rules/{ruleId}',
    method: 'delete',
    invalidatePrefix: '/v2/projects/{projectId}/ai-review-rules',
  });

  const isDryRun = rule.mode === 'DRY_RUN';
  const promptName =
    (rule.promptId != null && promptNames[rule.promptId]) ||
    t('ai_review_rule_default_prompt', 'Default review prompt');
  const scoreInfo = rule.never
    ? t('ai_review_rule_never_info', 'Never auto-review')
    : t('ai_review_rule_score_info', 'Score ≥ {threshold}', {
        threshold: rule.threshold,
      });

  function handleToggle() {
    enabledLoadable.mutate({
      path: { projectId: project.id, ruleId: rule.id },
      content: { 'application/json': { enabled: !rule.enabled } },
    });
  }

  function handleDelete() {
    setMenuOpen(false);
    confirmation({
      message: <T keyName="ai_review_rule_delete_message" params={{ label }} />,
      onConfirm() {
        deleteLoadable.mutate(
          { path: { projectId: project.id, ruleId: rule.id } },
          {
            onSuccess() {
              messageService.success(
                <T keyName="ai_review_rule_delete_success" />
              );
            },
          }
        );
      },
    });
  }

  return (
    <StyledItem
      variant="outlined"
      data-cy="ai-review-rule-item"
      data-cy-name={label}
    >
      <Box display="grid" gap={1}>
        <Typography variant="h6" data-cy="ai-review-rule-label">
          {label}
        </Typography>
        <AiReviewRuleScopeChips rule={rule} />
        <StyledInfoLine>
          {scoreInfo}
          {rule.overrides.length > 0 &&
            ' · ' +
              t('ai_review_rule_overrides_info', '{count} language overrides', {
                count: rule.overrides.length,
              })}
          {' · '}
          {promptName}
        </StyledInfoLine>
        {isDryRun ? (
          <StyledInfoLine data-cy="ai-review-rule-counts-dry-run">
            {t(
              'ai_review_rule_counts_dry_run',
              '{scored} scored · {wouldBeAutoReviewed} would be auto-reviewed',
              {
                scored: rule.counts.scored,
                wouldBeAutoReviewed: rule.counts.wouldBeAutoReviewed,
              }
            )}
          </StyledInfoLine>
        ) : (
          <StyledInfoLine data-cy="ai-review-rule-counts-active">
            {t(
              'ai_review_rule_counts_active',
              '{autoReviewed} auto-reviewed · {rejected} rejected · {failed} failed',
              {
                autoReviewed: rule.counts.autoReviewed,
                rejected: rule.counts.rejected,
                failed: rule.counts.failed,
              }
            )}
          </StyledInfoLine>
        )}
        {rule.counts.backlog > 0 && (
          <StyledInfoLine data-cy="ai-review-rule-backlog">
            {isDryRun
              ? t(
                  'ai_review_rule_backlog_dry_run',
                  '{count} existing translations not scored',
                  { count: rule.counts.backlog }
                )
              : t(
                  'ai_review_rule_backlog_active',
                  '{count} existing translations not reviewed',
                  { count: rule.counts.backlog }
                )}
          </StyledInfoLine>
        )}
      </Box>
      <Box display="flex" alignItems="start" gap={1}>
        <Chip
          size="small"
          data-cy="ai-review-rule-mode"
          data-cy-mode={rule.mode}
          label={
            isDryRun
              ? t('ai_review_rule_mode_dry_run', 'Dry run')
              : t('ai_review_rule_mode_active', 'Active')
          }
          color={isDryRun ? 'default' : 'primary'}
        />
        <Switch
          size="small"
          checked={rule.enabled}
          disabled={!canEdit || enabledLoadable.isLoading}
          onChange={handleToggle}
          data-cy="ai-review-rule-switch"
          data-cy-name={label}
        />
        {canEdit && (
          <>
            <IconButton
              size="small"
              onClick={() => setEditOpen(true)}
              data-cy="ai-review-rule-edit"
              data-cy-name={label}
            >
              <Edit02 width={18} height={18} />
            </IconButton>
            <IconButton
              size="small"
              ref={menuButtonRef}
              onClick={() => setMenuOpen(true)}
              data-cy="ai-review-rule-menu"
              data-cy-name={label}
            >
              <DotsVertical width={18} height={18} />
            </IconButton>
          </>
        )}
      </Box>
      {menuOpen && (
        <Menu
          anchorEl={menuButtonRef.current}
          open
          onClose={() => setMenuOpen(false)}
        >
          <MenuItem onClick={handleDelete} data-cy="ai-review-rule-menu-delete">
            {t('ai_review_rule_delete', 'Delete')}
          </MenuItem>
        </Menu>
      )}
      {editOpen && (
        <AiReviewRuleDialog rule={rule} onClose={() => setEditOpen(false)} />
      )}
    </StyledItem>
  );
};
