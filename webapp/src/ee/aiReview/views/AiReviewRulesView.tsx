import { useState } from 'react';
import { Box, Button, Paper, styled, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Plus } from '@untitled-ui/icons-react';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { DisabledFeatureBanner } from 'tg.component/common/DisabledFeatureBanner';
import { EmptyState } from 'tg.component/common/EmptyState';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { invalidateUrlPrefix, useApiQuery } from 'tg.service/http/useQueryApi';

import { useAiReviewEnabled } from 'tg.ee.module/aiReview/hooks/useAiReviewEnabled';
import { useAiReviewRules } from 'tg.ee.module/aiReview/hooks/useAiReviewRules';
import { AiReviewRuleDialog } from 'tg.ee.module/aiReview/components/AiReviewRuleDialog';
import { AiReviewRuleItem } from 'tg.ee.module/aiReview/components/AiReviewRuleItem';
import { AiReviewRuleModel } from 'tg.ee.module/aiReview/utils/types';
import { getRuleRunLanguageIds } from 'tg.ee.module/aiReview/utils/ruleLanguages';
import { AiReviewRunDialog } from 'tg.ee.module/aiReview/components/AiReviewRunDialog';
import { useAiReviewRuleBacklogs } from 'tg.ee.module/aiReview/hooks/useAiReviewRuleBacklogs';
import { useAiReviewRunPermission } from 'tg.ee.module/aiReview/hooks/useAiReviewRunPermission';
import { useAiReviewRuleDisplayName } from 'tg.ee.module/aiReview/hooks/useAiReviewRuleDisplayName';
import { BatchOperationDialog } from 'tg.views/projects/translations/BatchOperations/OperationsSummary/BatchOperationDialog';
import { useProjectActions } from 'tg.hooks/ProjectContext';
import { useQueryClient } from 'react-query';
import { components } from 'tg.service/apiSchema.generated';

const StyledHeader = styled('div')`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing(3)};
`;

const StyledList = styled(Paper)`
  display: grid;
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
`;

type BatchJobModel = components['schemas']['BatchJobModel'];

type DialogState = { rule?: AiReviewRuleModel } | undefined;

type RunDialogState =
  | {
      rule: AiReviewRuleModel;
      justActivated: boolean;
      dismissWhenNothingToReview: boolean;
    }
  | undefined;

export const AiReviewRulesView = () => {
  const { t } = useTranslate();
  const project = useProject();
  const { satisfiesPermission, satisfiesLanguageAccess } =
    useProjectPermissions();
  const { available, organizationLoaded } = useAiReviewEnabled();
  const canEdit = satisfiesPermission('prompts.edit');
  const [dialog, setDialog] = useState<DialogState>();
  const [runDialog, setRunDialog] = useState<RunDialogState>();
  const [runningJob, setRunningJob] = useState<BatchJobModel>();
  const canRunReview = useAiReviewRunPermission() && canEdit;
  const { refetchBatchJobs } = useProjectActions();
  const queryClient = useQueryClient();
  const getRuleDisplayName = useAiReviewRuleDisplayName();
  const backlogs = useAiReviewRuleBacklogs(project.id, canRunReview);

  const rulesLoadable = useAiReviewRules(project.id);

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
    options: { enabled: available },
  });

  const description = t(
    'ai_review_description',
    'AI review scores translations with an LLM and marks the good ones as reviewed. Keys that match no rule are never reviewed automatically. When several rules match, the strictest wins.'
  );

  if (!organizationLoaded) {
    return <BoxLoading />;
  }

  if (!available) {
    return (
      <Box mt={4} display="grid" gap={2}>
        <Typography variant="h5">
          {t('ai_review_title', 'AI review')}
        </Typography>
        <DisabledFeatureBanner customMessage={description} />
      </Box>
    );
  }

  if (rulesLoadable.isLoading || languagesLoadable.isLoading) {
    return <BoxLoading />;
  }

  const languages =
    languagesLoadable.data?._embedded?.languages?.filter((l) => !l.base) ?? [];

  function getRunLanguageIds(rule: AiReviewRuleModel) {
    return getRuleRunLanguageIds(
      rule,
      languages.map((l) => l.id),
      (id) => satisfiesLanguageAccess('translations.state-edit', id)
    );
  }

  function canRunRule(rule: AiReviewRuleModel) {
    return canRunReview && getRunLanguageIds(rule).length > 0;
  }

  function offerRun(rule: AiReviewRuleModel, previous?: AiReviewRuleModel) {
    if (!canRunRule(rule) || !rule.enabled) {
      return;
    }
    const wasActive = previous?.enabled && previous.mode === 'ACTIVE';
    setRunDialog({
      rule,
      justActivated: rule.mode === 'ACTIVE' && !wasActive,
      dismissWhenNothingToReview: true,
    });
  }
  const rules = rulesLoadable.data?._embedded?.rules ?? [];

  const addButton = canEdit && (
    <Button
      color="primary"
      variant="contained"
      startIcon={<Plus width={19} height={19} />}
      onClick={() => setDialog({})}
      data-cy="ai-review-add-rule"
      sx={{ flexShrink: 0 }}
    >
      {t('ai_review_add_rule', 'Add rule')}
    </Button>
  );

  return (
    <Box mt={4} mb={3} display="grid" gap={3}>
      <StyledHeader>
        <Box display="grid" gap={1}>
          <Typography variant="h5">
            {t('ai_review_title', 'AI review')}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {description}
          </Typography>
        </Box>
        {rules.length > 0 && addButton}
      </StyledHeader>

      {rules.length ? (
        <StyledList elevation={0} data-cy="ai-review-rules-list">
          {rules.map((rule) => (
            <AiReviewRuleItem
              key={rule.id}
              rule={rule}
              languages={languages}
              projectId={project.id}
              canEdit={canEdit}
              canRunReview={canRunRule(rule)}
              backlog={backlogs.byRuleId.get(rule.id)}
              onEdit={() => setDialog({ rule })}
              onEnabled={(saved) => offerRun(saved, rule)}
              onReviewExisting={() =>
                setRunDialog({
                  rule,
                  justActivated: false,
                  dismissWhenNothingToReview: false,
                })
              }
            />
          ))}
        </StyledList>
      ) : (
        <EmptyState>
          <Box
            display="grid"
            justifyItems="center"
            gap={2}
            textAlign="center"
            data-cy="ai-review-rules-empty"
          >
            <Typography variant="h4">
              {t('ai_review_empty_title', 'No AI review rules yet')}
            </Typography>
            <Typography color="text.secondary" maxWidth={520}>
              {t(
                'ai_review_empty_description',
                'Add a rule to choose which translations AI can review. Start in dry run to see the scores before anything gets marked as reviewed.'
              )}
            </Typography>
            {addButton}
          </Box>
        </EmptyState>
      )}

      {dialog && (
        <AiReviewRuleDialog
          projectId={project.id}
          languages={languages}
          rule={dialog.rule}
          onClose={() => setDialog(undefined)}
          onSaved={offerRun}
        />
      )}

      {runDialog && (
        <AiReviewRunDialog
          projectId={project.id}
          variant="rule"
          request={{
            ruleId: runDialog.rule.id,
            languageIds: getRunLanguageIds(runDialog.rule),
          }}
          rule={{
            name: getRuleDisplayName(runDialog.rule, languages),
            dryRun: runDialog.rule.mode === 'DRY_RUN',
            justActivated: runDialog.justActivated,
          }}
          dismissWhenNothingToReview={runDialog.dismissWhenNothingToReview}
          onClose={() => setRunDialog(undefined)}
          onStarted={(job) => {
            setRunDialog(undefined);
            setRunningJob(job);
            refetchBatchJobs();
          }}
        />
      )}

      {runningJob && (
        <BatchOperationDialog
          operation={runningJob}
          onClose={() => setRunningJob(undefined)}
          onFinished={() => {
            invalidateUrlPrefix(
              queryClient,
              '/v2/projects/{projectId}/ai-review/rules'
            );
          }}
        />
      )}
    </Box>
  );
};
