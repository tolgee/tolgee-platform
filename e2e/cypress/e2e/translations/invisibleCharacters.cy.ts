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
      login('franta', 'admin');
      createKey(projectId, 'nbsp key', { en: 'Bonjour\u00A0!' });
      createKey(projectId, 'zero width key', { en: 'Save\u200Bchanges' });
      createKey(projectId, 'plain key', { en: 'Plain value' });
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
      value: 'invisible-character',
      kind: 'nonBreakingSpace',
    }).should('have.length.at.least', 1);
  });

  it('marks a zero-width space in a translation cell', () => {
    visitTranslations(projectId);
    waitForGlobalLoading();
    gcyAdvanced({
      value: 'invisible-character',
      kind: 'zeroWidth',
    }).should('have.length.at.least', 1);
  });

  it('does not mark regular spaces', () => {
    visitTranslations(projectId);
    waitForGlobalLoading();
    gcyAdvanced({
      value: 'translations-table-cell',
      key: 'plain key',
      language: 'en',
    })
      .find('[data-cy="invisible-character"]')
      .should('not.exist');
  });

  it('marks a non-breaking space inside the editor', () => {
    visitTranslations(projectId);
    waitForGlobalLoading();
    editCell('Bonjour');
    gcy('global-editor')
      .find('.cm-invisible-char-nbsp')
      .should('have.length.at.least', 1);
  });

  it('marks a zero-width space inside the editor', () => {
    visitTranslations(projectId);
    waitForGlobalLoading();
    editCell('Save');
    gcy('global-editor')
      .find('.cm-invisible-char-zero-width')
      .should('have.length.at.least', 1);
  });
});
