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
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useProjectContext } from 'tg.hooks/ProjectContext';
import { AiReviewRuleItem } from 'tg.ee.module/aiReview/rules/AiReviewRuleItem';
import { AiReviewRuleDialog } from 'tg.ee.module/aiReview/rules/dialog/AiReviewRuleDialog';
import {
  AiReviewRunDialog,
  AiReviewRunVariant,
  runVariantForMode,
} from 'tg.ee.module/aiReview/runs/AiReviewRunDialog';

const enabledFirstThenNewest = (
  a: { enabled: boolean; id: number },
  b: { enabled: boolean; id: number }
) => Number(b.enabled) - Number(a.enabled) || b.id - a.id;

export const AiReviewRulesView = () => {
  const { t } = useTranslate();
  const theme = useTheme();
  const project = useProject();
  const { satisfiesPermission } = useProjectPermissions();
  const canEdit = satisfiesPermission('prompts.edit');
  const canRun = satisfiesPermission('translations.batch-machine');
  const [createOpen, setCreateOpen] = useState(false);
  const [globalRunOpen, setGlobalRunOpen] = useState(false);
  const [run, setRun] = useState<{
    ruleId: number;
    variant: AiReviewRunVariant;
  }>();

  const rulesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/ai-review-rules',
    method: 'get',
    path: { projectId: project.id },
  });

  const enabledMutation = useApiMutation({
    url: '/v2/projects/{projectId}/ai-review-rules/enabled',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}',
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

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
  });
  const runLanguages = (
    languagesLoadable.data?._embedded?.languages ?? []
  ).filter((l) => !l.base);

  if (rulesLoadable.isLoading) {
    return <BoxLoading />;
  }

  const rules = [...(rulesLoadable.data?._embedded?.aiReviewRules ?? [])].sort(
    enabledFirstThenNewest
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
              checked={project.useAiReview}
              onChange={(_, checked) =>
                enabledMutation.mutate({
                  path: { projectId: project.id },
                  content: { 'application/json': { enabled: checked } },
                })
              }
              disabled={!canEdit || enabledMutation.isLoading}
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
        <Box display="grid" gap={2.5}>
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
            <Box display="flex" alignItems="center" gap={1} flexShrink={0}>
              {canRun && rules.length > 0 && (
                <Button
                  variant="outlined"
                  onClick={() => setGlobalRunOpen(true)}
                  disabled={!project.useAiReview}
                  data-cy="ai-review-run-button"
                  sx={{ whiteSpace: 'nowrap' }}
                >
                  {t('ai_review_run_button', 'Run review')}
                </Button>
              )}
              {canEdit && rules.length > 0 && (
                <Button
                  color="primary"
                  variant="contained"
                  startIcon={<Plus width={19} height={19} />}
                  onClick={() => setCreateOpen(true)}
                  disabled={!project.useAiReview}
                  data-cy="ai-review-add-rule-button"
                  sx={{ whiteSpace: 'nowrap' }}
                >
                  {t('ai_review_add_rule', 'Add rule')}
                </Button>
              )}
            </Box>
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
                    disabled={!project.useAiReview}
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
                borderRadius: `${theme.shape.borderRadius * 2}px`,
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
        {globalRunOpen && (
          <AiReviewRunDialog
            variant="review"
            request={{ languageIds: runLanguages.map((l) => l.id) }}
            onClose={() => setGlobalRunOpen(false)}
          />
        )}
      </Box>
    </ProjectLanguagesProvider>
  );
};
