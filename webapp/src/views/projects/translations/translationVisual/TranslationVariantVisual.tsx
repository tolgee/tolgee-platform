import { useMemo } from 'react';
import { TolgeeFormat } from '@tginternal/editor';

import { adjustQaIssuesForVariant } from 'tg.fixtures/qaUtils';
import { LimitedHeightText } from 'tg.component/LimitedHeightText';
import { TranslationWithPlaceholders } from 'tg.views/projects/translations/translationVisual/TranslationWithPlaceholders';
import { components } from 'tg.service/apiSchema.generated';

type QaIssueModel = components['schemas']['QaIssueModel'];

type Props = {
  value: TolgeeFormat;
  variant?: string;
  qaIssues: QaIssueModel[];
  maxLines?: number;
  width?: number | string;
  content: string | undefined;
  exampleValue?: number;
  locale: string;
  targetLocale?: string;
  showHighlights?: boolean;
  translationId?: number;
};

export const TranslationVariantVisual = ({
  value,
  variant,
  qaIssues,
  maxLines,
  width,
  content,
  exampleValue,
  locale,
  targetLocale,
  showHighlights,
  translationId,
}: Props) => {
  const offset = value.variantOffsets?.[variant as Intl.LDMLPluralRule] ?? 0;
  const variantQaIssues = useMemo(
    () => adjustQaIssuesForVariant(qaIssues, variant, offset),
    [qaIssues, variant, offset]
  );
  return (
    <LimitedHeightText
      maxLines={maxLines === undefined ? 3 : maxLines!}
      width={width}
      lineHeight="1.3em"
    >
      <TranslationWithPlaceholders
        content={content || ''}
        pluralExampleValue={exampleValue}
        locale={locale}
        targetLocale={targetLocale}
        nested={Boolean(variant)}
        showHighlights={showHighlights}
        qaIssues={variantQaIssues}
        translationId={translationId}
      />
    </LimitedHeightText>
  );
};
