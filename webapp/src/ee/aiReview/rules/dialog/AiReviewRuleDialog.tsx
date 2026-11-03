import {
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  styled,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Formik } from 'formik';
import * as Yup from 'yup';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useIsBranchingEnabled } from 'tg.component/branching/useIsBranchingEnabled';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { TextField } from 'tg.component/common/form/fields/TextField';

import { RuleKeysScopeSelect } from 'tg.ee.module/aiReview/rules/dialog/RuleKeysScopeSelect';
import { RuleBranchesField } from 'tg.ee.module/aiReview/rules/dialog/RuleBranchesField';
import { RuleThresholdField } from 'tg.ee.module/aiReview/rules/dialog/RuleThresholdField';
import { RuleLanguageOverrides } from 'tg.ee.module/aiReview/rules/dialog/RuleLanguageOverrides';
import { RulePromptSelect } from 'tg.ee.module/aiReview/rules/dialog/RulePromptSelect';
import { RuleModeSelect } from 'tg.ee.module/aiReview/rules/dialog/RuleModeSelect';
import { RuleLanguagesField } from 'tg.ee.module/aiReview/rules/dialog/RuleLanguagesField';
import {
  getRuleInitialValues,
  hasThresholdOverride,
  RuleFormValues,
  ruleNeverFullyApplies,
  ruleValuesToRequest,
} from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';

type AiReviewRuleModel = components['schemas']['AiReviewRuleModel'];

const StyledDialogContent = styled(DialogContent)`
  display: grid;
  gap: ${({ theme }) => theme.spacing(2.5)};
  margin-top: ${({ theme }) => theme.spacing(1)};
  width: 85vw;
  max-width: 600px;
`;

const StyledPromptModeRow = styled('div')`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: ${({ theme }) => theme.spacing(1.5)};
  align-items: start;
`;

type Props = {
  onClose: () => void;
  rule?: AiReviewRuleModel;
  onRequestRun?: (saved: AiReviewRuleModel) => void;
};

export const AiReviewRuleDialog = ({ onClose, rule, onRequestRun }: Props) => {
  const { t } = useTranslate();
  const project = useProject();
  const messaging = useMessage();
  const isBranchingEnabled = useIsBranchingEnabled();
  const { satisfiesPermission } = useProjectPermissions();
  const canRun = satisfiesPermission('translations.batch-machine');

  function offerRun(saved: AiReviewRuleModel) {
    if (!onRequestRun || !canRun || saved.counts.backlog === 0) {
      return;
    }
    onRequestRun(saved);
  }

  const createMutation = useApiMutation({
    url: '/v2/projects/{projectId}/ai-review-rules',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/ai-review-rules',
  });

  const updateMutation = useApiMutation({
    url: '/v2/projects/{projectId}/ai-review-rules/{ruleId}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/ai-review-rules',
  });

  const thresholdMessage = t(
    'ai_review_rule_form_threshold_invalid',
    'Enter a number between 0 and 100'
  );

  const validationSchema = Yup.object({
    threshold: Yup.number()
      .typeError(thresholdMessage)
      .min(0, thresholdMessage)
      .max(100, thresholdMessage),
  });

  function handleSaved(data: AiReviewRuleModel) {
    messaging.success(
      rule ? (
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
        initialValues={getRuleInitialValues(rule)}
        validationSchema={validationSchema}
        validateOnBlur={false}
        onSubmit={handleSubmit}
      >
        {({ isSubmitting, handleSubmit, values, setFieldValue }) => {
          const fullNever = ruleNeverFullyApplies(values);
          const overrideHintVisible =
            values.never && hasThresholdOverride(values);
          return (
            <>
              <DialogTitle>
                {rule
                  ? t('ai_review_rule_dialog_edit_title', 'Edit AI review rule')
                  : t(
                      'ai_review_rule_dialog_create_title',
                      'Create AI review rule'
                    )}
              </DialogTitle>
              <StyledDialogContent>
                <TextField
                  name="name"
                  size="small"
                  minHeight={false}
                  label={t('ai_review_rule_form_name_label', 'Name')}
                  data-cy="ai-review-rule-form-name"
                />
                <RuleKeysScopeSelect
                  value={{
                    tagIn: values.tagIn,
                    tagNotIn: values.tagNotIn,
                    namespaceIn: values.namespaceIn,
                    namespaceNotIn: values.namespaceNotIn,
                  }}
                  onChange={(scope) => {
                    setFieldValue('tagIn', scope.tagIn);
                    setFieldValue('tagNotIn', scope.tagNotIn);
                    setFieldValue('namespaceIn', scope.namespaceIn);
                    setFieldValue('namespaceNotIn', scope.namespaceNotIn);
                  }}
                />
                <RuleLanguagesField />
                {isBranchingEnabled && (
                  <RuleBranchesField
                    branchIn={values.branchIn}
                    branchNotIn={values.branchNotIn}
                    onChange={(branches) => {
                      setFieldValue('branchIn', branches.branchIn);
                      setFieldValue('branchNotIn', branches.branchNotIn);
                    }}
                  />
                )}
                <RuleThresholdField />
                <RuleLanguageOverrides />
                <StyledPromptModeRow>
                  <RulePromptSelect />
                  <RuleModeSelect disabled={fullNever} />
                </StyledPromptModeRow>
                {overrideHintVisible && (
                  <Typography
                    variant="caption"
                    color="textSecondary"
                    data-cy="ai-review-rule-form-override-hint"
                  >
                    {t(
                      'ai_review_rule_form_override_only_hint',
                      'Applies only to languages with an override'
                    )}
                  </Typography>
                )}
                {project.useQaChecks && (
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={values.skipOpenQaIssues}
                        disabled={fullNever}
                        onChange={(_, checked) =>
                          setFieldValue('skipOpenQaIssues', checked)
                        }
                        data-cy="ai-review-rule-form-skip-qa"
                      />
                    }
                    label={t(
                      'ai_review_rule_form_skip_qa_label',
                      "Don't auto-review translations with open QA issues"
                    )}
                  />
                )}
              </StyledDialogContent>
              <DialogActions>
                <Button onClick={onClose} data-cy="ai-review-rule-form-cancel">
                  {t('ai_review_rule_form_cancel', 'Cancel')}
                </Button>
                <LoadingButton
                  loading={isSubmitting}
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
