import { useState } from 'react';
import {
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  styled,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { TextField } from 'tg.component/common/TextField';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useQaChecksEnabled } from 'tg.ee.module/qa/hooks/useQaChecksEnabled';

import {
  AiReviewLanguageThreshold,
  AiReviewRuleMode,
  AiReviewRuleModel,
  AiReviewRuleRequest,
} from 'tg.ee.module/aiReview/utils/types';
import {
  AiReviewScopeField,
  AiReviewScopeValue,
} from 'tg.ee.module/aiReview/components/AiReviewScopeField';
import {
  AiReviewBranchField,
  AiReviewBranchValue,
} from 'tg.ee.module/aiReview/components/AiReviewBranchField';
import { useIsBranchingEnabled } from 'tg.component/branching/useIsBranchingEnabled';
import {
  AiReviewThresholdField,
  getEffectiveOverrides,
  parseThreshold,
} from 'tg.ee.module/aiReview/components/AiReviewThresholdField';
import {
  AiReviewPromptField,
  AiReviewPromptValue,
} from 'tg.ee.module/aiReview/components/AiReviewPromptField';
import { AiReviewModeField } from 'tg.ee.module/aiReview/components/AiReviewModeField';

type LanguageModel = components['schemas']['LanguageModel'];

const DEFAULT_THRESHOLD = 85;

const StyledContent = styled(DialogContent)`
  display: grid;
  gap: ${({ theme }) => theme.spacing(2.5)};
  width: 600px;
  max-width: 100%;
  box-sizing: border-box;
`;

const StyledTwoColumns = styled('div')`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: ${({ theme }) => theme.spacing(1.5)};
  align-items: start;
`;

type Props = {
  projectId: number;
  languages: LanguageModel[];
  rule?: AiReviewRuleModel;
  onClose: () => void;
  onSaved?: (saved: AiReviewRuleModel, previous?: AiReviewRuleModel) => void;
};

export const AiReviewRuleDialog = ({
  projectId,
  languages,
  rule,
  onClose,
  onSaved,
}: Props) => {
  const { t } = useTranslate();
  const qaChecksEnabled = useQaChecksEnabled();

  const [name, setName] = useState(rule?.name ?? '');
  const [threshold, setThreshold] = useState<string>(
    String(rule?.threshold ?? DEFAULT_THRESHOLD)
  );
  const [mode, setMode] = useState<AiReviewRuleMode>(rule?.mode ?? 'DRY_RUN');
  const [promptId, setPromptId] = useState<AiReviewPromptValue>(
    rule?.promptId ?? 'default'
  );
  const [skipWithOpenQaIssues, setSkipWithOpenQaIssues] = useState(
    rule?.skipWithOpenQaIssues ?? true
  );
  const branchingEnabled = useIsBranchingEnabled();
  const [branches, setBranches] = useState<AiReviewBranchValue>(() => ({
    branchIn: rule?.branchIn ?? [],
    branchNotIn: rule?.branchNotIn ?? [],
  }));
  const [scope, setScope] = useState<AiReviewScopeValue>(() => ({
    filters: {
      filterTag: nonEmpty(rule?.tagIn),
      filterNoTag: nonEmpty(rule?.tagNotIn),
      filterNamespace: nonEmpty(rule?.namespaceIn),
      filterNoNamespace: nonEmpty(rule?.namespaceNotIn),
    },
    languageTags: languages
      .filter((l) => rule?.languageIds.includes(l.id))
      .map((l) => l.tag),
  }));
  const [overrides, setOverrides] = useState<AiReviewLanguageThreshold[]>(
    () =>
      rule?.languageThresholds.filter((o) =>
        languages.some((l) => l.id === o.languageId)
      ) ?? []
  );

  const createLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/ai-review/rules',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/ai-review/rules',
  });

  const updateLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/ai-review/rules/{ruleId}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/ai-review/rules',
  });

  const parsedThreshold = parseThreshold(threshold);
  const overrideLanguages = scope.languageTags.length
    ? languages.filter((l) => scope.languageTags.includes(l.tag))
    : languages;

  function buildRequest(thresholdValue: number): AiReviewRuleRequest {
    const languageIds = languages
      .filter((l) => scope.languageTags.includes(l.tag))
      .map((l) => l.id);
    return {
      name: name.trim() || undefined,
      enabled: rule?.enabled ?? true,
      mode,
      threshold: thresholdValue,
      promptId: promptId === 'default' ? undefined : promptId,
      skipWithOpenQaIssues,
      tagIn: scope.filters.filterTag ?? [],
      tagNotIn: scope.filters.filterNoTag ?? [],
      namespaceIn: scope.filters.filterNamespace ?? [],
      namespaceNotIn: scope.filters.filterNoNamespace ?? [],
      branchIn: branches.branchIn,
      branchNotIn: branches.branchNotIn,
      languageIds,
      languageThresholds: getEffectiveOverrides(
        overrides,
        overrideLanguages
      ).map((o) =>
        o.never
          ? { languageId: o.languageId, never: true }
          : { languageId: o.languageId, never: false, threshold: o.threshold }
      ),
    };
  }

  function handleSubmit() {
    if (parsedThreshold === undefined) {
      return;
    }
    const content = { 'application/json': buildRequest(parsedThreshold) };
    const onSuccess = (saved: AiReviewRuleModel) => {
      messageService.success(
        rule ? (
          <T
            keyName="ai_review_rule_update_success"
            defaultValue="Rule saved"
          />
        ) : (
          <T
            keyName="ai_review_rule_create_success"
            defaultValue="Rule created"
          />
        )
      );
      onClose();
      onSaved?.(saved, rule);
    };
    if (rule) {
      updateLoadable.mutate(
        { path: { projectId, ruleId: rule.id }, content },
        { onSuccess }
      );
    } else {
      createLoadable.mutate({ path: { projectId }, content }, { onSuccess });
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      maxWidth="md"
      data-cy="ai-review-rule-dialog"
    >
      <DialogTitle>
        {rule
          ? t('ai_review_rule_dialog_edit_title', 'Edit AI review rule')
          : t('ai_review_rule_dialog_create_title', 'New AI review rule')}
      </DialogTitle>
      <StyledContent>
        <TextField
          label={t('ai_review_rule_name_label', 'Name (optional)')}
          value={name}
          onChange={(e) => setName(e.target.value)}
          minHeight={false}
          fullWidth
          data-cy="ai-review-rule-name-field"
        />
        <AiReviewScopeField
          projectId={projectId}
          languages={languages}
          value={scope}
          onChange={setScope}
        />
        {(branchingEnabled ||
          Boolean(branches.branchIn.length + branches.branchNotIn.length)) && (
          <AiReviewBranchField
            projectId={projectId}
            value={branches}
            onChange={setBranches}
          />
        )}
        <AiReviewThresholdField
          threshold={threshold}
          initialThreshold={rule?.threshold ?? DEFAULT_THRESHOLD}
          onThresholdChange={setThreshold}
          overrides={overrides}
          onOverridesChange={setOverrides}
          overrideLanguages={overrideLanguages}
        />
        <StyledTwoColumns>
          <AiReviewPromptField
            projectId={projectId}
            value={promptId}
            onChange={setPromptId}
          />
          <AiReviewModeField value={mode} onChange={setMode} />
        </StyledTwoColumns>
        {qaChecksEnabled && (
          <FormControlLabel
            control={
              <Checkbox
                size="small"
                checked={skipWithOpenQaIssues}
                onChange={(e) => setSkipWithOpenQaIssues(e.target.checked)}
              />
            }
            label={t(
              'ai_review_rule_skip_qa_label',
              "Don't auto-review translations with open QA issues"
            )}
            data-cy="ai-review-rule-skip-qa-checkbox"
          />
        )}
      </StyledContent>
      <DialogActions>
        <Button onClick={onClose} data-cy="ai-review-rule-cancel">
          {t('global_cancel_button')}
        </Button>
        <LoadingButton
          variant="contained"
          color="primary"
          loading={createLoadable.isLoading || updateLoadable.isLoading}
          disabled={parsedThreshold === undefined}
          onClick={handleSubmit}
          data-cy="ai-review-rule-save"
        >
          {t('global_form_save')}
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
};

function nonEmpty(list: string[] | undefined) {
  return list?.length ? list : undefined;
}
