import { useEffect, useState } from 'react';
import { Box, Button, Link, Switch, Typography, useTheme } from '@mui/material';
import { DOCS_LINKS } from 'tg.constants/docLinks';
import { useTranslate } from '@tolgee/react';
import { Plus } from '@untitled-ui/icons-react';

import { EmptyState } from 'tg.component/common/EmptyState';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { ProjectLanguagesProvider } from 'tg.hooks/ProjectLanguagesProvider';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProjectContext } from 'tg.hooks/ProjectContext';
import { AiReviewRuleItem } from 'tg.ee.module/aiReview/rules/AiReviewRuleItem';
import { AiReviewRuleDialog } from 'tg.ee.module/aiReview/rules/dialog/AiReviewRuleDialog';
import {
  AiReviewRunDialog,
  AiReviewRunVariant,
  runVariantForMode,
} from 'tg.ee.module/aiReview/runs/AiReviewRunDialog';

export const AiReviewRulesView = () => {
  const { t } = useTranslate();
  const theme = useTheme();
  const project = useProject();
  const { satisfiesPermission } = useProjectPermissions();
  const canEdit = satisfiesPermission('prompts.edit');
  const [createOpen, setCreateOpen] = useState(false);
  // TODO(backend): a project-level AI review switch like `useQaChecks`; local state for the design.
  const [aiReviewEnabled, setAiReviewEnabled] = useState(false);
  const disabled = !aiReviewEnabled;
  const [run, setRun] = useState<{
    ruleId: number;
    variant: AiReviewRunVariant;
  }>();

  const rulesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/ai-review-rules',
    method: 'get',
    path: { projectId: project.id },
  });

  const batchOperations = useProjectContext((c) => c.batchOperations);
  const finishedAiReviewJobs =
    batchOperations?.filter(
      (operation) =>
        operation.type === 'AI_REVIEW' && operation.status === 'SUCCESS'
    ).length ?? 0;

  useEffect(() => {
    if (finishedAiReviewJobs > 0) {
      rulesLoadable.refetch();
    }
  }, [finishedAiReviewJobs]);

  const promptsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/prompts',
    method: 'get',
    path: { projectId: project.id },
    query: { type: 'REVIEW', size: 1000 },
  });

  if (rulesLoadable.isLoading) {
    return <BoxLoading />;
  }

  // Newest first, so a just-saved rule is on top; disabled rules go to the end.
  const rules = [...(rulesLoadable.data?._embedded?.aiReviewRules ?? [])].sort(
    (a, b) => Number(b.enabled) - Number(a.enabled) || b.id - a.id
  );
  const promptNames = Object.fromEntries(
    (promptsLoadable.data?._embedded?.prompts ?? []).map((prompt) => [
      prompt.id,
      prompt.name,
    ])
  );

  return (
    <ProjectLanguagesProvider>
      <Box
        display="grid"
        marginTop={4}
        gap={2.5}
        data-cy="ai-review-rules-view"
      >
        <Box display="grid" gap={1}>
          <Box display="flex" alignItems="center" gap={1}>
            <Typography variant="h5">
              {t('ai_review_global_title', 'AI review')}
            </Typography>
            <Switch
              size="small"
              checked={aiReviewEnabled}
              onChange={(_, checked) => setAiReviewEnabled(checked)}
              disabled={!canEdit}
              data-cy="ai-review-enabled-toggle"
            />
          </Box>
          <Typography variant="body2" color={theme.palette.text.secondary}>
            {t(
              'ai_review_global_description',
              'AI helps you review translations faster while you stay in control. Set rules for specific tags, namespaces and languages: let AI score translations, mark those with a good score as reviewed, or exclude them from AI review.'
            )}{' '}
            <Link
              href={DOCS_LINKS.aiReview}
              target="_blank"
              rel="noopener noreferrer"
              data-cy="ai-review-docs-link"
            >
              {t('ai_review_global_docs', 'Learn more')}
            </Link>
          </Typography>
        </Box>
        <Box
          display="grid"
          gap={2.5}
          // Like QA checks: the rules stay visible but can't be used until AI review is on.
          sx={{
            transition: 'opacity 0.2s',
            opacity: disabled ? 0.5 : 1,
            pointerEvents: disabled ? 'none' : undefined,
          }}
          aria-disabled={disabled}
        >
          <Box
            display="flex"
            flexWrap="wrap"
            justifyContent="space-between"
            alignItems="center"
            gap={2}
          >
            <Typography variant="h6">
              {t('ai_review_rules_title', 'Rules')}
            </Typography>
            {canEdit && rules.length > 0 && (
              <Button
                color="primary"
                variant="contained"
                startIcon={<Plus width={19} height={19} />}
                onClick={() => setCreateOpen(true)}
                data-cy="ai-review-add-rule-button"
                sx={{ flexShrink: 0, whiteSpace: 'nowrap' }}
              >
                {t('ai_review_add_rule', 'Add rule')}
              </Button>
            )}
          </Box>
          {rules.length === 0 ? (
            <EmptyState>
              <Box
                display="grid"
                justifyItems="center"
                gap={2}
                textAlign="center"
                data-cy="ai-review-empty-state"
              >
                <Typography variant="h4">
                  {t('ai_review_rules_empty_message', 'No AI review rules yet')}
                </Typography>
                <Typography color={theme.palette.text.secondary}>
                  {t(
                    'ai_review_rules_empty_description',
                    'Create a rule to let the AI review new translations for you.'
                  )}
                </Typography>
                {canEdit && (
                  <Button
                    color="primary"
                    variant="contained"
                    startIcon={<Plus width={19} height={19} />}
                    onClick={() => setCreateOpen(true)}
                    data-cy="ai-review-add-rule-button"
                  >
                    {t('ai_review_add_rule', 'Add rule')}
                  </Button>
                )}
              </Box>
            </EmptyState>
          ) : (
            <Box
              display="grid"
              sx={(theme) => ({
                border: `1px solid ${theme.palette.tokens.border.secondary}`,
                borderRadius: '8px',
                overflow: 'hidden',
              })}
            >
              {rules.map((rule) => (
                <AiReviewRuleItem
                  key={rule.id}
                  rule={rule}
                  promptNames={promptNames}
                />
              ))}
            </Box>
          )}
        </Box>
        {createOpen && (
          <AiReviewRuleDialog
            onClose={() => setCreateOpen(false)}
            onRequestRun={(saved) =>
              setRun({
                ruleId: saved.id,
                variant: runVariantForMode(saved.mode),
              })
            }
          />
        )}
        {run && (
          <AiReviewRunDialog
            variant={run.variant}
            request={{ ruleId: run.ruleId }}
            onClose={() => setRun(undefined)}
          />
        )}
      </Box>
    </ProjectLanguagesProvider>
  );
};
