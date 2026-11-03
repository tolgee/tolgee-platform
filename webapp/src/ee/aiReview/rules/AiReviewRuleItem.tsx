import { useRef, useState } from 'react';
import {
  Box,
  Chip,
  IconButton,
  Link as MuiLink,
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
import { AiReviewRuleScopeChips } from 'tg.ee.module/aiReview/rules/AiReviewRuleScopeChips';
import { AiReviewRuleDialog } from 'tg.ee.module/aiReview/rules/dialog/AiReviewRuleDialog';
import {
  AiReviewRunDialog,
  AiReviewRunVariant,
} from 'tg.ee.module/aiReview/runs/AiReviewRunDialog';
import { useRuleLabel } from 'tg.ee.module/aiReview/rules/useRuleLabel';

type AiReviewRuleModel = components['schemas']['AiReviewRuleModel'];

const StyledItem = styled(Paper)`
  display: grid;
  grid-template-columns: 1fr auto;
  gap: ${({ theme }) => theme.spacing(1)};
  padding: ${({ theme }) => theme.spacing(2)};
`;

const StyledInfoLine = styled(Typography)`
  display: flex;
  flex-wrap: wrap;
  column-gap: ${({ theme }) => theme.spacing(0.5)};
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: ${({ theme }) => theme.typography.body2.fontSize};
`;

const Dot = () => <span aria-hidden="true">·</span>;

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
  const [runVariant, setRunVariant] = useState<AiReviewRunVariant>();
  const menuButtonRef = useRef<HTMLButtonElement>(null);
  const canRun = satisfiesPermission('translations.batch-machine');

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
      message: (
        <T
          keyName="ai_review_rule_delete_message"
          defaultValue="Delete the AI review rule {label}?"
          params={{ label }}
        />
      ),
      onConfirm() {
        deleteLoadable.mutate(
          { path: { projectId: project.id, ruleId: rule.id } },
          {
            onSuccess() {
              messageService.success(
                <T
                  keyName="ai_review_rule_delete_success"
                  defaultValue="Rule deleted"
                />
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
          <span>{scoreInfo}</span>
          {rule.overrides.length > 0 && (
            <>
              <Dot />
              <span>
                {t(
                  'ai_review_rule_overrides_info',
                  '{count, plural, one {# language override} other {# language overrides}}',
                  { count: rule.overrides.length }
                )}
              </span>
            </>
          )}
          <Dot />
          <span>{promptName}</span>
        </StyledInfoLine>
        {isDryRun ? (
          <StyledInfoLine data-cy="ai-review-rule-counts-dry-run">
            {t(
              'ai_review_rule_counts_dry_run',
              '{scored, plural, one {# scored} other {# scored}} · {wouldBeAutoReviewed, plural, one {# would be auto-reviewed} other {# would be auto-reviewed}}',
              {
                scored: rule.counts.scored,
                wouldBeAutoReviewed: rule.counts.wouldBeAutoReviewed,
              }
            )}
          </StyledInfoLine>
        ) : (
          <StyledInfoLine
            data-cy="ai-review-rule-counts-active"
            data-cy-failed={String(rule.counts.failed)}
          >
            <span>
              {t(
                'ai_review_rule_counts_active',
                '{autoReviewed, plural, one {# auto-reviewed} other {# auto-reviewed}} · {rejected, plural, one {# rejected} other {# rejected}} · {failed, plural, one {# failed} other {# failed}}',
                {
                  autoReviewed: rule.counts.autoReviewed,
                  rejected: rule.counts.rejected,
                  failed: rule.counts.failed,
                }
              )}
            </span>
            {canRun && rule.counts.failed > 0 && (
              <>
                <Dot />
                <MuiLink
                  component="button"
                  onClick={() => setRunVariant('retry')}
                  data-cy="ai-review-rule-retry-failed"
                >
                  {t('ai_review_rule_retry_failed', 'Retry failed reviews')}
                </MuiLink>
              </>
            )}
          </StyledInfoLine>
        )}
        {rule.counts.backlog > 0 && (
          <StyledInfoLine data-cy="ai-review-rule-backlog">
            <span>
              {isDryRun
                ? t(
                    'ai_review_rule_backlog_dry_run',
                    '{count, plural, one {# existing translation not scored} other {# existing translations not scored}}',
                    { count: rule.counts.backlog }
                  )
                : t(
                    'ai_review_rule_backlog_active',
                    '{count, plural, one {# existing translation not reviewed} other {# existing translations not reviewed}}',
                    { count: rule.counts.backlog }
                  )}
            </span>
            {canRun && (
              <>
                <Dot />
                <MuiLink
                  component="button"
                  onClick={() => setRunVariant(isDryRun ? 'score' : 'review')}
                  data-cy="ai-review-rule-review-now"
                >
                  {isDryRun
                    ? t('ai_review_rule_score_now', 'Score now')
                    : t('ai_review_rule_review_now', 'Review now')}
                </MuiLink>
              </>
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
        <AiReviewRuleDialog
          rule={rule}
          onClose={() => setEditOpen(false)}
          onRequestRun={(saved) =>
            setRunVariant(saved.mode === 'DRY_RUN' ? 'score' : 'review')
          }
        />
      )}
      {runVariant && (
        <AiReviewRunDialog
          variant={runVariant}
          request={{ ruleId: rule.id }}
          onClose={() => setRunVariant(undefined)}
        />
      )}
    </StyledItem>
  );
};
