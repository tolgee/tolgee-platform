import { useRef, useState } from 'react';
import {
  Box,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  styled,
  Switch,
  Tooltip,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { DotsVertical, Edit02 } from '@untitled-ui/icons-react';
import clsx from 'clsx';

import { components } from 'tg.service/apiSchema.generated';
import { confirmation } from 'tg.hooks/confirmation';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { AiReviewRuleModel } from 'tg.ee.module/aiReview/utils/types';
import { ruleToRequest } from 'tg.ee.module/aiReview/utils/ruleRequest';
import { AiReviewScopeChips } from 'tg.ee.module/aiReview/components/AiReviewScopeChips';
import { useAiReviewRuleDisplayName } from 'tg.ee.module/aiReview/hooks/useAiReviewRuleDisplayName';
import { AiReviewRuleStats } from 'tg.ee.module/aiReview/components/AiReviewRuleStats';
import { AiReviewRuleBacklog } from 'tg.ee.module/aiReview/components/AiReviewRuleBacklog';
import { StyledRuleDetails } from 'tg.ee.module/aiReview/components/AiReviewRuleRowStyles';

type LanguageModel = components['schemas']['LanguageModel'];

const StyledItem = styled('div')`
  display: grid;
  grid-template-columns: 1fr auto;
  gap: ${({ theme }) => theme.spacing(1, 2)};
  padding: ${({ theme }) => theme.spacing(2)};
  cursor: pointer;
  transition: background 0.1s ease-in-out;

  &:not(:last-of-type) {
    border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
  }

  &.readOnly {
    cursor: default;
  }

  &:not(.readOnly):hover {
    background: ${({ theme }) => theme.palette.tokens.text._states.hover};
  }

  &.disabled .content,
  &.disabled .mode {
    opacity: 0.5;
  }
`;

const StyledContent = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(1)};
  min-width: 0;
`;

const StyledActions = styled('div')`
  display: flex;
  align-items: center;
  align-self: start;
  gap: ${({ theme }) => theme.spacing(0.5)};
`;

type Props = {
  rule: AiReviewRuleModel;
  languages: LanguageModel[];
  projectId: number;
  canEdit: boolean;
  canRunReview: boolean;
  backlog: number | undefined;
  onEdit: () => void;
  onEnabled: (rule: AiReviewRuleModel) => void;
  onReviewExisting: () => void;
};

export const AiReviewRuleItem = ({
  rule,
  languages,
  projectId,
  canEdit,
  canRunReview,
  backlog,
  onEdit,
  onEnabled,
  onReviewExisting,
}: Props) => {
  const { t } = useTranslate();
  const [menuOpen, setMenuOpen] = useState(false);
  const menuAnchor = useRef<HTMLButtonElement>(null);

  const updateLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/ai-review/rules/{ruleId}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/ai-review/rules',
  });

  const retryFailedLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/ai-review/rules/{ruleId}/retry-failed',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/ai-review/rules',
  });

  const deleteLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/ai-review/rules/{ruleId}',
    method: 'delete',
    invalidatePrefix: '/v2/projects/{projectId}/ai-review/rules',
  });

  const getRuleDisplayName = useAiReviewRuleDisplayName();
  const displayName = getRuleDisplayName(rule, languages);

  const existingLanguageIds = new Set(languages.map((l) => l.id));
  const overrides = rule.languageThresholds.filter((o) =>
    existingLanguageIds.has(o.languageId)
  );

  function handleToggleEnabled() {
    updateLoadable.mutate(
      {
        path: { projectId, ruleId: rule.id },
        content: {
          'application/json': {
            ...ruleToRequest(rule, existingLanguageIds),
            enabled: !rule.enabled,
          },
        },
      },
      {
        onSuccess(saved) {
          if (saved.enabled) {
            onEnabled(saved);
          }
        },
      }
    );
  }

  function handleReviewExisting() {
    setMenuOpen(false);
    onReviewExisting();
  }

  function handleRetryFailed() {
    setMenuOpen(false);
    retryFailedLoadable.mutate(
      { path: { projectId, ruleId: rule.id } },
      {
        onSuccess(data) {
          messageService.success(
            <T
              keyName="ai_review_rule_retry_failed_success"
              defaultValue="{count, plural, one {# review} other {# reviews}} queued again"
              params={{ count: data.queued }}
            />
          );
        },
      }
    );
  }

  function handleDelete() {
    setMenuOpen(false);
    confirmation({
      title: (
        <T
          keyName="ai_review_rule_delete_title_named"
          defaultValue="Delete rule {name}?"
          params={{ name: displayName }}
        />
      ),
      message: (
        <T
          keyName="ai_review_rule_delete_message"
          defaultValue="Matching translations will no longer be reviewed by AI. Translations already marked as reviewed stay reviewed."
        />
      ),
      confirmButtonText: (
        <T keyName="ai_review_rule_delete_confirm" defaultValue="Delete" />
      ),
      onConfirm() {
        deleteLoadable.mutate(
          { path: { projectId, ruleId: rule.id } },
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

  const overridesTooltip = overrides
    .map((o) => {
      const language = languages.find((l) => l.id === o.languageId);
      return { language, override: o };
    })
    .map(({ language, override }) => (
      <div key={override.languageId}>
        {override.never
          ? t('ai_review_rule_override_never_line', '{languageName}: never', {
              languageName: language?.name ?? '',
            })
          : t(
              'ai_review_rule_override_threshold_line',
              '{languageName}: {threshold}',
              {
                languageName: language?.name ?? '',
                threshold: override.threshold ?? 0,
              }
            )}
      </div>
    ));

  return (
    <StyledItem
      className={clsx({ disabled: !rule.enabled, readOnly: !canEdit })}
      onClick={canEdit ? onEdit : undefined}
      data-cy="ai-review-rule-item"
      data-cy-name={rule.name ?? ''}
    >
      <StyledContent className="content">
        <Typography
          variant="body1"
          fontWeight={500}
          data-cy="ai-review-rule-title"
        >
          {displayName}
        </Typography>
        <AiReviewScopeChips rule={rule} languages={languages} />
        <StyledRuleDetails>
          <span data-cy="ai-review-rule-threshold">
            {t('ai_review_rule_threshold_summary', 'Score ≥ {threshold}', {
              threshold: rule.threshold,
            })}
          </span>
          {overrides.length > 0 && (
            <>
              <span className="separator">·</span>
              <Tooltip title={<>{overridesTooltip}</>} disableInteractive>
                <span data-cy="ai-review-rule-overrides">
                  {t(
                    'ai_review_rule_overrides_hint',
                    '{count, plural, one {# language override} other {# language overrides}}',
                    { count: overrides.length }
                  )}
                </span>
              </Tooltip>
            </>
          )}
          <span className="separator">·</span>
          <span data-cy="ai-review-rule-prompt">
            {rule.promptName ??
              t('ai_review_default_prompt', 'Default review prompt')}
          </span>
        </StyledRuleDetails>
        <AiReviewRuleStats
          rule={rule}
          canEdit={canEdit}
          onRetryFailed={handleRetryFailed}
        />
        {rule.enabled && canRunReview && Boolean(backlog) && (
          <AiReviewRuleBacklog
            rule={rule}
            backlog={backlog ?? 0}
            onRun={onReviewExisting}
          />
        )}
      </StyledContent>
      <StyledActions onClick={(e) => e.stopPropagation()}>
        <Chip
          size="small"
          variant="outlined"
          color={rule.mode === 'ACTIVE' ? 'success' : 'default'}
          label={
            rule.mode === 'ACTIVE'
              ? t('ai_review_rule_mode_active', 'Active')
              : t('ai_review_rule_mode_dry_run', 'Dry run')
          }
          className="mode"
          data-cy="ai-review-rule-mode"
          data-cy-mode={rule.mode}
        />
        {canEdit && (
          <>
            <Tooltip
              title={
                rule.enabled
                  ? t('ai_review_rule_disable', 'Disable rule')
                  : t('ai_review_rule_enable', 'Enable rule')
              }
              disableInteractive
            >
              <Switch
                size="small"
                checked={rule.enabled}
                onChange={handleToggleEnabled}
                disabled={updateLoadable.isLoading}
                data-cy="ai-review-rule-enabled-switch"
              />
            </Tooltip>
            <IconButton
              size="small"
              onClick={onEdit}
              data-cy="ai-review-rule-edit"
            >
              <Edit02 width={18} height={18} />
            </IconButton>
            <IconButton
              size="small"
              ref={menuAnchor}
              onClick={() => setMenuOpen(true)}
              data-cy="ai-review-rule-menu"
            >
              <DotsVertical width={18} height={18} />
            </IconButton>
          </>
        )}
        {!canEdit && !rule.enabled && (
          <Box sx={{ fontSize: 13, color: 'text.secondary' }}>
            {t('ai_review_rule_disabled', 'Disabled')}
          </Box>
        )}
      </StyledActions>
      {menuOpen && (
        <Menu
          anchorEl={menuAnchor.current}
          open
          onClose={() => setMenuOpen(false)}
          onClick={(e) => e.stopPropagation()}
        >
          {rule.enabled && canRunReview && (
            <MenuItem
              onClick={handleReviewExisting}
              data-cy="ai-review-rule-review-existing"
            >
              {t(
                'ai_review_rule_menu_review_existing',
                'Review existing translations'
              )}
            </MenuItem>
          )}
          {rule.stats.failed > 0 && (
            <MenuItem
              onClick={handleRetryFailed}
              disabled={retryFailedLoadable.isLoading}
              data-cy="ai-review-rule-retry-failed"
            >
              {t('ai_review_rule_menu_retry_failed', 'Retry failed reviews')}
            </MenuItem>
          )}
          <MenuItem onClick={handleDelete} data-cy="ai-review-rule-delete">
            {t('ai_review_rule_menu_delete', 'Delete')}
          </MenuItem>
        </Menu>
      )}
    </StyledItem>
  );
};
