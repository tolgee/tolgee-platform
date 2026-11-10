import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  styled,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Formik } from 'formik';
import { useQueryClient } from 'react-query';

import { components } from 'tg.service/apiSchema.generated';
import { matchUrlPrefix, useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { TextField } from 'tg.component/common/form/fields/TextField';

import { RuleAppliesTo } from 'tg.ee.module/aiReview/rules/dialog/RuleAppliesTo';
import { RuleThresholdField } from 'tg.ee.module/aiReview/rules/dialog/RuleThresholdField';
import { RulePromptSelect } from 'tg.ee.module/aiReview/rules/dialog/RulePromptSelect';
import { RuleQaCheckbox } from 'tg.ee.module/aiReview/rules/dialog/RuleQaCheckbox';
import { RuleModeSelect } from 'tg.ee.module/aiReview/rules/dialog/RuleModeSelect';
import {
  RuleNeverCheckbox,
  RuleNeverInfo,
} from 'tg.ee.module/aiReview/rules/dialog/RuleNeverCheckbox';
import {
  getRuleInitialValues,
  RuleFormValues,
  ruleValuesToRequest,
} from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';

type AiReviewRuleModel = components['schemas']['AiReviewRuleModel'];
type RulesList = {
  _embedded?: { aiReviewRules?: AiReviewRuleModel[] };
};

const RULES_URL = '/v2/projects/{projectId}/ai-review-rules';

const StyledDialogContent = styled(DialogContent)`
  display: grid;
  gap: ${({ theme }) => theme.spacing(2)};
  margin-top: ${({ theme }) => theme.spacing(1)};
  width: 85vw;
  max-width: 600px;
`;

type Props = {
  onClose: () => void;
  rule?: AiReviewRuleModel;
  /** Creates a new rule prefilled from this one. */
  duplicateOf?: AiReviewRuleModel;
  onRequestRun?: (saved: AiReviewRuleModel) => void;
};

// "Checkout" -> "Checkout 2", "Checkout 2" -> "Checkout 3"; an unnamed rule stays unnamed.
function duplicateName(name?: string) {
  if (!name) {
    return '';
  }
  const numbered = name.match(/^(.*\S)\s+(\d+)$/);
  return numbered ? `${numbered[1]} ${Number(numbered[2]) + 1}` : `${name} 2`;
}

export const AiReviewRuleDialog = ({
  onClose,
  rule,
  duplicateOf,
  onRequestRun,
}: Props) => {
  const { t } = useTranslate();
  const project = useProject();
  const messaging = useMessage();
  const { satisfiesPermission } = useProjectPermissions();
  const canRun = satisfiesPermission('translations.batch-machine');

  const queryClient = useQueryClient();

  async function offerRun(saved: AiReviewRuleModel) {
    await queryClient.invalidateQueries(matchUrlPrefix(RULES_URL));
    // A rule that keeps AI away has nothing to score, so there is no run to offer.
    if (saved.never || !onRequestRun || !canRun) {
      return;
    }
    const rules = queryClient.getQueryData<RulesList>([
      RULES_URL,
      { projectId: project.id },
      undefined,
      undefined,
    ]);
    const refreshed = rules?._embedded?.aiReviewRules?.find(
      (r) => r.id === saved.id
    );
    if (!refreshed?.counts?.backlog) {
      return;
    }
    onRequestRun(refreshed);
  }

  const createMutation = useApiMutation({
    url: '/v2/projects/{projectId}/ai-review-rules',
    method: 'post',
  });

  const updateMutation = useApiMutation({
    url: '/v2/projects/{projectId}/ai-review-rules/{ruleId}',
    method: 'put',
  });

  function handleSaved(data: AiReviewRuleModel) {
    messaging.success(
      data.never ? (
        <T
          keyName="ai_review_rule_never_saved"
          defaultValue="Selected translations are excluded from AI review"
        />
      ) : rule ? (
        <T keyName="ai_review_rule_update_success" defaultValue="Rule saved" />
      ) : (
        <T
          keyName="ai_review_rule_create_success"
          defaultValue="Rule created"
        />
      )
    );
    onClose();
    offerRun(data);
  }

  function handleSubmit(values: RuleFormValues) {
    const request = ruleValuesToRequest(values, rule?.enabled ?? true);
    if (rule) {
      return updateMutation.mutateAsync(
        {
          path: { projectId: project.id, ruleId: rule.id },
          content: { 'application/json': request },
        },
        { onSuccess: handleSaved }
      );
    }
    return createMutation.mutateAsync(
      {
        path: { projectId: project.id },
        content: { 'application/json': request },
      },
      { onSuccess: handleSaved }
    );
  }

  return (
    <Dialog
      open={true}
      onClose={onClose}
      maxWidth="md"
      data-cy="ai-review-rule-dialog"
    >
      <Formik
        initialValues={
          duplicateOf
            ? {
                ...getRuleInitialValues(duplicateOf),
                name: duplicateName(duplicateOf.name),
              }
            : getRuleInitialValues(rule)
        }
        onSubmit={handleSubmit}
      >
        {({ isSubmitting, handleSubmit, values }) => {
          return (
            <>
              <DialogTitle
                sx={{
                  display: 'flex',
                  flexWrap: 'wrap',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  columnGap: 2,
                }}
              >
                <span>
                  {rule
                    ? t(
                        'ai_review_rule_dialog_edit_title',
                        'Edit AI review rule'
                      )
                    : duplicateOf
                    ? t(
                        'ai_review_rule_dialog_duplicate_title',
                        'Duplicate AI review rule'
                      )
                    : t(
                        'ai_review_rule_dialog_create_title',
                        'Create AI review rule'
                      )}
                </span>
                <RuleNeverCheckbox />
              </DialogTitle>
              <StyledDialogContent>
                <TextField
                  name="name"
                  size="small"
                  minHeight={false}
                  label={t('ai_review_rule_form_rule_name_label', 'Rule name')}
                  data-cy="ai-review-rule-form-name"
                />
                <RuleAppliesTo />
                <RuleNeverInfo />
                {!values.never && (
                  <>
                    <RulePromptSelect />
                    <Box display="grid" gap={0.5}>
                      <RuleModeSelect />
                      {values.mode === 'ACTIVE' && (
                        // A light panel across the form for the settings of the selected mode.
                        <Box
                          display="grid"
                          gap={1}
                          mt={0.5}
                          px={2}
                          py={1.5}
                          sx={(theme) => ({
                            borderRadius: `${theme.shape.borderRadius * 2}px`,
                            background: theme.palette.tokens.text._states.hover,
                          })}
                        >
                          <RuleThresholdField />
                        </Box>
                      )}
                      {/* Applies to both modes: AI skips these translations entirely. */}
                      <RuleQaCheckbox />
                    </Box>
                  </>
                )}
              </StyledDialogContent>
              <DialogActions>
                <Button onClick={onClose} data-cy="ai-review-rule-form-cancel">
                  {t('ai_review_rule_form_cancel', 'Cancel')}
                </Button>
                <LoadingButton
                  loading={isSubmitting}
                  // The name is how rules are told apart in the list; a rule needs a language.
                  disabled={!values.name.trim() || values.noLanguages}
                  variant="contained"
                  color="primary"
                  onClick={() => handleSubmit()}
                  data-cy="ai-review-rule-form-save"
                >
                  {t('ai_review_rule_form_save', 'Save')}
                </LoadingButton>
              </DialogActions>
            </>
          );
        }}
      </Formik>
    </Dialog>
  );
};
