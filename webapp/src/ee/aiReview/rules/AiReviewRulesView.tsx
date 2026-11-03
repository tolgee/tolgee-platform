import { useEffect, useState } from 'react';
import { Box, Button, Typography, useTheme } from '@mui/material';
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
import { SourceChangeSettings } from 'tg.ee.module/aiReview/settings/SourceChangeSettings';
import {
  AiReviewRunDialog,
  AiReviewRunVariant,
} from 'tg.ee.module/aiReview/runs/AiReviewRunDialog';

export const AiReviewRulesView = () => {
  const { t } = useTranslate();
  const theme = useTheme();
  const project = useProject();
  const { satisfiesPermission } = useProjectPermissions();
  const canEdit = satisfiesPermission('prompts.edit');
  const [createOpen, setCreateOpen] = useState(false);
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

  const rules = rulesLoadable.data?._embedded?.rules ?? [];
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
        <Box display="flex" justifyContent="space-between" alignItems="center">
          <Typography color={theme.palette.text.secondary}>
            {t(
              'ai_review_rules_description',
              'Rules decide which translations the AI reviews and when they are marked as reviewed.'
            )}
          </Typography>
          {canEdit && rules.length > 0 && (
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
          <Box display="grid" gap={1.5}>
            {rules.map((rule) => (
              <AiReviewRuleItem
                key={rule.id}
                rule={rule}
                promptNames={promptNames}
              />
            ))}
          </Box>
        )}
        <SourceChangeSettings />
        {createOpen && (
          <AiReviewRuleDialog
            onClose={() => setCreateOpen(false)}
            onRequestRun={(saved) =>
              setRun({
                ruleId: saved.id,
                variant: saved.mode === 'DRY_RUN' ? 'score' : 'review',
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
