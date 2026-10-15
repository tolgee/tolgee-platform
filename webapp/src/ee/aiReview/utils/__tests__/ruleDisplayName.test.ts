import { formatAiReviewRuleDisplayName } from 'tg.ee.module/aiReview/utils/ruleDisplayName';

const t = (
  _key: string,
  defaultValue: string,
  params?: Record<string, string | number>
) =>
  defaultValue.replace(/\{(\w+)}/g, (_, name) => String(params?.[name] ?? ''));

const languages = [
  { id: 1, name: 'German' },
  { id: 2, name: 'Czech' },
  { id: 3, name: 'French' },
];

const scope = {
  tagIn: [] as string[],
  tagNotIn: [] as string[],
  namespaceIn: [] as string[],
  namespaceNotIn: [] as string[],
  languageIds: [] as number[],
};

describe('formatAiReviewRuleDisplayName', () => {
  it('uses the rule name when present', () => {
    expect(
      formatAiReviewRuleDisplayName(
        { ...scope, name: ' Checkout ', tagIn: ['ui'] },
        languages,
        t
      )
    ).toBe('Checkout');
  });

  it('describes the scope of a rule without a name', () => {
    expect(
      formatAiReviewRuleDisplayName(
        {
          name: '',
          tagIn: ['checkout', 'ui'],
          tagNotIn: ['legal'],
          namespaceIn: ['checkout'],
          namespaceNotIn: [],
          languageIds: [1, 2],
        },
        languages,
        t
      )
    ).toBe(
      'Tags checkout, ui · Not tagged legal · Namespace checkout · German, Czech'
    );
  });

  it('describes an unrestricted rule', () => {
    expect(formatAiReviewRuleDisplayName(scope, languages, t)).toBe(
      'All keys · All languages'
    );
  });

  it('shortens long lists', () => {
    expect(
      formatAiReviewRuleDisplayName(
        {
          ...scope,
          tagIn: ['checkout', 'ui', 'a', 'b', 'c'],
          namespaceNotIn: [''],
          languageIds: [1, 2, 3],
        },
        languages,
        t
      )
    ).toBe(
      'Tags checkout, ui +3 · Not in namespace Without namespace · German, Czech +1'
    );
  });
});
