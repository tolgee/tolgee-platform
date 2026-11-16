import { mentionsToTokens } from 'tg.component/mentions/mentionTokens';

const anna = { id: 1, name: 'Anna' };
const ann = { id: 2, name: 'Ann' };

describe('mentionsToTokens', () => {
  it('returns the text unchanged when nobody was picked', () => {
    expect(mentionsToTokens('Hi @Anna', [])).toEqual('Hi @Anna');
  });

  it('replaces a picked name with its token', () => {
    expect(mentionsToTokens('Hi @Anna, look', [anna])).toEqual('Hi <@1>, look');
  });

  it('prefers the longer name when one name is a prefix of another', () => {
    expect(mentionsToTokens('@Ann and @Anna', [ann, anna])).toEqual(
      '<@2> and <@1>'
    );
  });

  it('does not match a name followed by a letter or digit', () => {
    expect(mentionsToTokens('@Annabel @Anna2 @Anna', [anna])).toEqual(
      '@Annabel @Anna2 <@1>'
    );
  });

  it('matches a name followed by punctuation', () => {
    expect(mentionsToTokens('@Anna?', [anna])).toEqual('<@1>?');
  });

  it('escapes regex characters in names', () => {
    const dotted = { id: 3, name: 'J. (Dev)' };
    expect(mentionsToTokens('@J. (Dev) vs @JX (Dev)', [dotted])).toEqual(
      '<@3> vs @JX (Dev)'
    );
  });

  it('matches names with non-latin letters', () => {
    const jiri = { id: 4, name: 'Jiří Čížek' };
    expect(mentionsToTokens('@Jiří Čížek!', [jiri])).toEqual('<@4>!');
  });

  it('gives each pick of two members with the same name its own id', () => {
    const first = { id: 10, name: 'Anna Novak' };
    const second = { id: 11, name: 'Anna Novak' };
    expect(
      mentionsToTokens('@Anna Novak and @Anna Novak', [first, second])
    ).toEqual('<@10> and <@11>');
  });

  it('maps repeated mentions of one picked member to the same id', () => {
    expect(mentionsToTokens('@Anna @Anna', [anna])).toEqual('<@1> <@1>');
  });
});
