import {
  applyQaReplacement,
  adjustQaIssuesForVariant,
  QaVariantIssue,
} from '../qaUtils';

describe('applyQaReplacement', () => {
  it('replaces text at middle of string', () => {
    const result = applyQaReplacement('Hello world!', {
      positionStart: 6,
      positionEnd: 11,
      replacement: 'earth',
    });
    expect(result).toBe('Hello earth!');
  });

  it('replaces text at start of string', () => {
    const result = applyQaReplacement('Hello world!', {
      positionStart: 0,
      positionEnd: 5,
      replacement: 'Hi',
    });
    expect(result).toBe('Hi world!');
  });

  it('replaces text at end of string', () => {
    const result = applyQaReplacement('Hello world!', {
      positionStart: 11,
      positionEnd: 12,
      replacement: '.',
    });
    expect(result).toBe('Hello world.');
  });

  it('handles zero-length issue (insertion)', () => {
    const result = applyQaReplacement('Hello world', {
      positionStart: 5,
      positionEnd: 5,
      replacement: ',',
    });
    expect(result).toBe('Hello, world');
  });

  it('returns unchanged text when no positions', () => {
    const result = applyQaReplacement('Hello world', {
      replacement: 'ignored',
    });
    expect(result).toBe('Hello world');
  });

  it('returns unchanged text when positionStart is null', () => {
    const result = applyQaReplacement('Hello world', {
      positionStart: undefined,
      positionEnd: 5,
      replacement: 'test',
    });
    expect(result).toBe('Hello world');
  });

  it('handles deletion when replacement is undefined', () => {
    const result = applyQaReplacement('Hello  world', {
      positionStart: 5,
      positionEnd: 6,
      replacement: undefined,
    });
    expect(result).toBe('Hello world');
  });
});

describe('adjustQaIssuesForVariant', () => {
  const makeIssue = (
    start: number,
    end: number,
    variant?: string
  ): QaVariantIssue => ({
    positionStart: start,
    positionEnd: end,
    pluralVariant: variant,
  });

  it('adjusts positions by offset', () => {
    const issues = [makeIssue(10, 15, 'one')];
    const result = adjustQaIssuesForVariant(issues, 'one', 5);
    expect(result[0].positionStart).toBe(5);
    expect(result[0].positionEnd).toBe(10);
  });

  it('returns original issues when offset is zero', () => {
    const issues = [makeIssue(10, 15, 'one')];
    const result = adjustQaIssuesForVariant(issues, 'one', 0);
    expect(result).toBe(issues); // same reference
  });

  it('returns original issues when variant is undefined', () => {
    const issues = [makeIssue(10, 15)];
    const result = adjustQaIssuesForVariant(issues, undefined, 5);
    expect(result).toBe(issues);
  });

  it('filters to matching variant and issues without variant', () => {
    const issues = [
      makeIssue(10, 15, 'one'),
      makeIssue(20, 25, 'other'),
      makeIssue(30, 35), // no variant — included
    ];
    const result = adjustQaIssuesForVariant(issues, 'one', 5);
    expect(result).toHaveLength(2);
    expect(result[0].positionStart).toBe(5); // from 'one' variant
    expect(result[1].positionStart).toBe(25); // from no-variant issue
  });

  it('handles multiple issues adjusted correctly', () => {
    const issues = [makeIssue(100, 105, 'other'), makeIssue(110, 120, 'other')];
    const result = adjustQaIssuesForVariant(issues, 'other', 50);
    expect(result).toHaveLength(2);
    expect(result[0].positionStart).toBe(50);
    expect(result[0].positionEnd).toBe(55);
    expect(result[1].positionStart).toBe(60);
    expect(result[1].positionEnd).toBe(70);
  });

  it('preserves undefined positions', () => {
    const issue: QaVariantIssue = {
      positionStart: undefined,
      positionEnd: undefined,
      pluralVariant: 'one',
    };
    const result = adjustQaIssuesForVariant([issue], 'one', 5);
    expect(result[0].positionStart).toBeUndefined();
    expect(result[0].positionEnd).toBeUndefined();
  });
});
