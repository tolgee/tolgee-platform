import { resolveSingleLineDoc, toSingleLineDoc } from '../singleLineDoc';

describe('resolveSingleLineDoc', () => {
  it('allows single-line changes', () => {
    expect(resolveSingleLineDoc('foo', 'foob', 1)).toEqual({
      action: 'allow',
    });
  });

  it('rejects a change that only inserts newlines (typed Enter)', () => {
    expect(resolveSingleLineDoc('foo', 'foo\n', 2)).toEqual({
      action: 'reject',
    });
    expect(resolveSingleLineDoc('foo', 'fo\no', 2)).toEqual({
      action: 'reject',
    });
  });

  it('flattens pasted multi-line content to one line', () => {
    expect(resolveSingleLineDoc('', 'key:\nfoo', 2)).toEqual({
      action: 'flatten',
      value: 'key: foo',
    });
    expect(resolveSingleLineDoc('a', 'a line1\n  line2\n', 3)).toEqual({
      action: 'flatten',
      value: 'a line1 line2 ',
    });
  });
});

describe('toSingleLineDoc', () => {
  it('flattens a multi-line initial value', () => {
    expect(toSingleLineDoc('key:foo\nde:bar')).toBe('key:foo de:bar');
  });

  it('keeps a single-line value untouched', () => {
    expect(toSingleLineDoc('key:foo')).toBe('key:foo');
  });

  it('flattens a newline-only value', () => {
    expect(toSingleLineDoc('\n')).toBe(' ');
  });
});
