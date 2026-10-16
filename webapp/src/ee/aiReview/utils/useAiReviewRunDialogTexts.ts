import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { RunPreviewState } from 'tg.ee.module/aiReview/utils/runPreview';

type AiReviewRunPreviewModel = components['schemas']['AiReviewRunPreviewModel'];

export type AiReviewRunDialogSubject =
  | { variant: 'selection'; keyCount: number; languageCount: number }
  | {
      variant: 'rule';
      rule: { name: string; dryRun: boolean; justActivated: boolean };
    };

export const useAiReviewRunDialogTexts = () => {
  const { t } = useTranslate();

  function getTitle(subject: AiReviewRunDialogSubject, state: RunPreviewState) {
    if (state.nothingToReview) {
      return t('ai_review_run_nothing_title', 'Nothing to review');
    }
    if (subject.variant === 'rule') {
      if (subject.rule.dryRun) {
        return t(
          'ai_review_run_rule_dry_run_title',
          'Score existing translations now?'
        );
      }
      return t('ai_review_run_rule_title', 'Review existing translations now?');
    }
    return t(
      'ai_review_run_title',
      '{count, plural, one {Review # translation with AI?} other {Review # translations with AI?}}',
      { count: state.willReview }
    );
  }

  function getRuleDescription(
    rule: { name: string; dryRun: boolean; justActivated: boolean },
    preview: AiReviewRunPreviewModel,
    state: RunPreviewState
  ) {
    const params = { name: rule.name, count: preview.total };
    if (state.nothingToReview) {
      return t(
        'ai_review_run_rule_nothing_description',
        '{count, plural, =0 {{name} covers no existing translations that AI could review.} one {{name} covers # existing translation, but AI can not review it now.} other {{name} covers # existing translations, but AI can not review any of them now.}}',
        params
      );
    }
    if (rule.dryRun && state.counts.dryRunOnly === state.willReview) {
      return t(
        'ai_review_run_rule_dry_run_description',
        '{name} is a Dry run rule and covers {count, plural, one {# existing translation} other {# existing translations}}. They get a score, their state does not change.',
        params
      );
    }
    if (rule.dryRun) {
      return t(
        'ai_review_run_rule_dry_run_mixed_description',
        '{name} is a Dry run rule and covers {count, plural, one {# existing translation} other {# existing translations}}. It only scores them, but where a stricter Active rule also matches, that rule decides and can mark the translation as reviewed.',
        params
      );
    }
    if (rule.justActivated) {
      return t(
        'ai_review_run_rule_activated_description',
        '{name} is now Active and covers {count, plural, one {# existing translation} other {# existing translations}}. New and changed translations are reviewed automatically. Existing ones are reviewed only when you start it.',
        params
      );
    }
    return t(
      'ai_review_run_rule_description',
      '{name} covers {count, plural, one {# existing translation} other {# existing translations}}. New and changed translations are reviewed automatically. Existing ones are reviewed only when you start it.',
      params
    );
  }

  function getDescription(
    subject: AiReviewRunDialogSubject,
    preview: AiReviewRunPreviewModel,
    state: RunPreviewState
  ) {
    if (subject.variant === 'rule') {
      return getRuleDescription(subject.rule, preview, state);
    }
    if (state.nothingToReview) {
      return t(
        'ai_review_run_nothing_description',
        '{count, plural, one {The selected translation can not be reviewed by AI.} other {None of the # selected translations can be reviewed by AI.}}',
        { count: preview.total }
      );
    }
    return t(
      'ai_review_run_selection_description',
      'You selected {keys, plural, one {# key} other {# keys}} in {languages, plural, one {# language} other {# languages}}, that is {count, plural, one {# translation} other {# translations}}.',
      {
        keys: subject.keyCount,
        languages: subject.languageCount,
        count: preview.total,
      }
    );
  }

  function getConfirmLabel(
    subject: AiReviewRunDialogSubject,
    state: RunPreviewState
  ) {
    if (subject.variant === 'rule') {
      if (subject.rule.dryRun) {
        return t('ai_review_run_score_now', 'Score now');
      }
      return t('ai_review_run_review_now', 'Review now');
    }
    return t(
      'ai_review_run_confirm',
      '{count, plural, one {Review # translation} other {Review # translations}}',
      { count: state.willReview }
    );
  }

  return { getTitle, getDescription, getConfirmLabel };
};
