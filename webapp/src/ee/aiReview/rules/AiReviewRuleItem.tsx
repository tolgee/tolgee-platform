import clsx from 'clsx';
import { useRef, useState } from 'react';
import {
  Box,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  Tooltip,
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
import { AiReviewRuleModeChip } from 'tg.ee.module/aiReview/rules/AiReviewRuleModeChip';
import { AiReviewRuleScopeChips } from 'tg.ee.module/aiReview/rules/AiReviewRuleScopeChips';
import { AiReviewRuleDialog } from 'tg.ee.module/aiReview/rules/dialog/AiReviewRuleDialog';
import {
  AiReviewRunDialog,
  AiReviewRunVariant,
  runVariantForMode,
} from 'tg.ee.module/aiReview/runs/AiReviewRunDialog';
import { AiReviewRuleCounts } from 'tg.ee.module/aiReview/rules/AiReviewRuleCounts';
import { stopBubble } from 'tg.fixtures/eventHandler';

type AiReviewRuleModel = components['schemas']['AiReviewRuleModel'];

const StyledItem = styled(Box)`
  display: grid;
  gap: ${({ theme }) => theme.spacing(1)};
  padding: ${({ theme }) => theme.spacing(2, 2.5)};

  &:not(:last-of-type) {
    border-bottom: 1px solid
      ${({ theme }) => theme.palette.tokens.border.secondary};
  }
  color: ${({ theme }) => theme.palette.text.primary};

  &.clickable {
    cursor: pointer;
  }

  .showOnHover {
    opacity: 0;
    transition: opacity ease-in-out 0.3s;
  }

  &:hover .showOnHover,
  &:focus-within .showOnHover,
  .showOnHover.open {
    opacity: 1;
  }

  &.clickable:hover,
  &.clickable:focus-within {
    background: ${({ theme }) => theme.palette.tokens.background.hover};
  }

  &.disabled .dim {
    opacity: 0.6;
    filter: grayscale(1);
  }
`;

const StyledRow = styled(Box)`
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
  min-width: 0;
`;

const StyledName = styled(Box)`
  font-size: ${({ theme }) => theme.typography.body1.fontSize}px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

type Props = {
  rule: AiReviewRuleModel;
  promptNames: Record<number, string>;
};

export const AiReviewRuleItem = ({ rule, promptNames }: Props) => {
  const { t } = useTranslate();
  const project = useProject();
  const { satisfiesPermission } = useProjectPermissions();
  const canEdit = satisfiesPermission('prompts.edit') && project.useAiReview;
  const label = rule.name;
  const [menuOpen, setMenuOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [duplicateOpen, setDuplicateOpen] = useState(false);
  const [duplicateRun, setDuplicateRun] = useState<{
    ruleId: number;
    variant: AiReviewRunVariant;
  }>();
  const [runVariant, setRunVariant] = useState<AiReviewRunVariant>();
  const menuButtonRef = useRef<HTMLButtonElement>(null);
  const canRun =
    satisfiesPermission('translations.batch-machine') && project.useAiReview;

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

  const customPromptName =
    rule.promptId != null ? promptNames[rule.promptId] : undefined;

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
      data-cy="ai-review-rule-item"
      data-cy-name={label}
      className={clsx({
        clickable: canEdit,
        disabled: !rule.enabled || !project.useAiReview,
      })}
      onClick={canEdit ? () => setEditOpen(true) : undefined}
    >
      <StyledRow>
        <Box display="flex" alignItems="center" gap={1} minWidth={0}>
          <Box
            display="flex"
            alignItems="center"
            gap={1}
            minWidth={0}
            className="dim"
          >
            <StyledName data-cy="ai-review-rule-label">{label}</StyledName>
            <AiReviewRuleModeChip rule={rule} />
          </Box>
          {!rule.enabled && (
            <Tooltip
              title={t(
                'ai_review_rule_disabled_hint',
                'This rule is off. Turn it on from the menu.'
              )}
              disableInteractive
            >
              <Chip
                size="small"
                variant="outlined"
                label={t('ai_review_rule_disabled', 'Disabled')}
                data-cy="ai-review-rule-disabled"
              />
            </Tooltip>
          )}
        </Box>
        <Box display="flex" alignItems="center" gap={1} onClick={stopBubble()}>
          {canEdit && (
            <IconButton
              size="small"
              className="showOnHover"
              onClick={() => setEditOpen(true)}
              data-cy="ai-review-rule-edit"
              data-cy-name={label}
              aria-label={t('ai_review_rule_edit_label', 'Edit rule')}
            >
              <Edit02 width={20} height={20} />
            </IconButton>
          )}
          {canEdit && (
            <IconButton
              size="small"
              ref={menuButtonRef}
              onClick={() => setMenuOpen(true)}
              data-cy="ai-review-rule-menu"
              data-cy-name={label}
              aria-label={t('ai_review_rule_menu_label', 'Rule actions')}
              sx={{ marginRight: -1 }}
            >
              <DotsVertical width={20} height={20} />
            </IconButton>
          )}
        </Box>
      </StyledRow>
      <StyledRow className="dim">
        <Box display="flex" alignItems="center" gap={1.5} minWidth={0}>
          <AiReviewRuleScopeChips rule={rule} />
          {!rule.never && customPromptName && (
            <Typography variant="body2" color="text.secondary" noWrap>
              {customPromptName}
            </Typography>
          )}
        </Box>
        {!rule.never && (
          <AiReviewRuleCounts
            rule={rule}
            canRun={canRun}
            onRun={setRunVariant}
          />
        )}
      </StyledRow>
      {menuOpen && (
        <Menu
          onClick={stopBubble()}
          anchorEl={menuButtonRef.current}
          open
          onClose={() => setMenuOpen(false)}
        >
          <MenuItem
            onClick={() => {
              setMenuOpen(false);
              handleToggle();
            }}
            disabled={enabledLoadable.isLoading}
            data-cy="ai-review-rule-switch"
            data-cy-name={label}
          >
            {rule.enabled
              ? t('ai_review_rule_menu_disable', 'Disable')
              : t('ai_review_rule_menu_enable', 'Enable')}
          </MenuItem>
          <MenuItem
            onClick={() => {
              setMenuOpen(false);
              setDuplicateOpen(true);
            }}
            data-cy="ai-review-rule-menu-duplicate"
          >
            {t('ai_review_rule_duplicate', 'Duplicate')}
          </MenuItem>
          <MenuItem onClick={handleDelete} data-cy="ai-review-rule-menu-delete">
            {t('ai_review_rule_delete', 'Delete')}
          </MenuItem>
        </Menu>
      )}
      <div style={{ display: 'contents' }} onClick={stopBubble()}>
        {editOpen && (
          <AiReviewRuleDialog
            rule={rule}
            onClose={() => setEditOpen(false)}
            onRequestRun={(saved) =>
              setRunVariant(runVariantForMode(saved.mode))
            }
          />
        )}
        {duplicateOpen && (
          <AiReviewRuleDialog
            duplicateOf={rule}
            onClose={() => setDuplicateOpen(false)}
            onRequestRun={(saved) =>
              setDuplicateRun({
                ruleId: saved.id,
                variant: runVariantForMode(saved.mode),
              })
            }
          />
        )}
        {duplicateRun && (
          <AiReviewRunDialog
            variant={duplicateRun.variant}
            request={{ ruleId: duplicateRun.ruleId }}
            onClose={() => setDuplicateRun(undefined)}
          />
        )}
        {runVariant && (
          <AiReviewRunDialog
            variant={runVariant}
            request={{ ruleId: rule.id }}
            onClose={() => setRunVariant(undefined)}
          />
        )}
      </div>
    </StyledItem>
  );
};
