import { createKey, login } from '../../common/apiCalls/common';
import { translationSingleTestData } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';
import { gcy, gcyAdvanced } from '../../common/shared';
import { editCell, visitTranslations } from '../../common/translations';

describe('Invisible characters', () => {
  let projectId: number;

  beforeEach(() => {
    translationSingleTestData.clean();
    translationSingleTestData.generate().then((data) => {
      projectId = data.body.id;
      // v2apiFetch reads the auth token when it builds the request options, so
      // these must be enqueued after login has resolved or they go out unauthenticated.
      login('franta', 'admin').then(() => {
        createKey(projectId, 'nbsp key', { en: 'Bonjour\u00A0!' });
        createKey(projectId, 'zero width key', { en: 'Zero\u200Bwidth' });
        createKey(projectId, 'plain key', { en: 'Plain value' });
      });
    });
  });

  afterEach(() => {
    waitForGlobalLoading();
    translationSingleTestData.clean();
  });

  it('marks a non-breaking space in a translation cell', () => {
    visitTranslations(projectId);
    waitForGlobalLoading();
    gcyAdvanced({
      value: 'translations-table-cell',
      key: 'nbsp key',
      language: 'en',
    })
      .findDcyAdvanced({
        value: 'invisible-character',
        kind: 'nonBreakingSpace',
      })
      .should('have.length.at.least', 1);
  });

  it('marks a zero-width space in a translation cell', () => {
    visitTranslations(projectId);
    waitForGlobalLoading();
    gcyAdvanced({
      value: 'translations-table-cell',
      key: 'zero width key',
      language: 'en',
    })
      .findDcyAdvanced({
        value: 'invisible-character',
        kind: 'zeroWidth',
      })
      .should('have.length.at.least', 1);
  });

  it('does not mark regular spaces', () => {
    visitTranslations(projectId);
    waitForGlobalLoading();
    gcyAdvanced({
      value: 'translations-table-cell',
      key: 'plain key',
      language: 'en',
    })
      .findDcy('invisible-character')
      .should('not.exist');
  });

  it('marks a non-breaking space inside the editor', () => {
    visitTranslations(projectId);
    waitForGlobalLoading();
    editCell('Bonjour');
    gcy('global-editor')
      .findDcyAdvanced({
        value: 'invisible-character-editor',
        kind: 'nonBreakingSpace',
      })
      .should('have.length.at.least', 1);
  });

  it('marks a zero-width space inside the editor', () => {
    visitTranslations(projectId);
    waitForGlobalLoading();
    editCell('Zero');
    gcy('global-editor')
      .findDcyAdvanced({
        value: 'invisible-character-editor',
        kind: 'zeroWidth',
      })
      .should('have.length.at.least', 1);
  });

  it('names the character in a hover tooltip', () => {
    visitTranslations(projectId);
    waitForGlobalLoading();
    editCell('Bonjour');
    gcy('global-editor')
      .findDcyAdvanced({
        value: 'invisible-character-editor',
        kind: 'nonBreakingSpace',
      })
      .first()
      .trigger('mousemove');
    gcy('invisible-character-tooltip').should('be.visible');
  });
});
