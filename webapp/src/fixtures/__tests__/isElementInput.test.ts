import { isElementInput } from '../isElementInput';

const element = (overrides: Record<string, unknown>) =>
  ({
    tagName: 'DIV',
    classList: { contains: () => false },
    isContentEditable: false,
    ...overrides,
  } as unknown as Element);

describe('isElementInput', () => {
  it('recognizes contentEditable elements like the CodeMirror search field', () => {
    expect(isElementInput(element({ isContentEditable: true }))).toBe(true);
  });

  it('rejects a plain element', () => {
    expect(isElementInput(element({}))).toBe(false);
  });

  it('recognizes textareas and text inputs', () => {
    expect(isElementInput(element({ tagName: 'TEXTAREA' }))).toBe(true);
    expect(isElementInput(element({ tagName: 'INPUT', type: 'text' }))).toBe(
      true
    );
  });

  it('rejects non-text inputs', () => {
    expect(
      isElementInput(element({ tagName: 'INPUT', type: 'checkbox' }))
    ).toBe(false);
  });
});
