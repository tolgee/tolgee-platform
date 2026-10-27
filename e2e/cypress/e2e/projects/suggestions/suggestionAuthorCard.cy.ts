import { login, setTranslations } from '../../../common/apiCalls/common';
import { communityStatsTestData } from '../../../common/apiCalls/testData/testData';
import { gcy, gcyAdvanced } from '../../../common/shared';
import {
  getTranslationCell,
  selectLangsInLocalstorage,
  visitTranslations,
} from '../../../common/translations';

describe('Suggestion author card', () => {
  let publicProjectId: number;
  let privateProjectId: number;
  let alice: { id: number; name: string };
  let dave: { id: number; name: string };

  beforeEach(() => {
    communityStatsTestData.clean();
    communityStatsTestData.generateStandard().then((r) => {
      const byName = (name: string) =>
        r.body.projects.find((p) => p.name === name)!.id;
      publicProjectId = byName('Community Stats Demo');
      privateProjectId = byName('Community Stats Private');
      alice = r.body.users.find((u) => u.username === 'alice@community.test')!;
      dave = r.body.users.find((u) => u.username === 'dave@community.test')!;
    });
  });

  afterEach(() => {
    communityStatsTestData.clean();
  });

  function hoverSuggestionAuthor(
    projectId: number,
    key: string,
    authorId: number
  ) {
    login('communityStatsOwner').then(() => {
      setTranslations(projectId, key, { cs: 'Překlad' });
    });
    selectLangsInLocalstorage(projectId, ['en', 'cs']);
    visitTranslations(projectId);
    getTranslationCell(key, 'cs').click();
    gcy('translation-tools-suggestions-show-all-checkbox').click();
    gcyAdvanced({
      value: 'translation-suggestion',
      'author-id': String(authorId),
    })
      .first()
      .findDcy('suggestion-author')
      .trigger('mouseover');
  }

  it('shows the contributor card with a profile link on a community project', () => {
    hoverSuggestionAuthor(publicProjectId, 'key-1', alice.id);
    gcy('suggestion-author-card')
      .should('be.visible')
      .and('contain', alice.name);
    // Alice in the community-stats seed: 15 suggested, 6 accepted, 4 declined
    gcy('suggestion-author-card-stats')
      .invoke('text')
      .should('match', /^6 .*\b15\b.*\b4\b/);
    gcy('suggestion-author-card-stats').find('b').should('have.text', '6');
    gcy('suggestion-author-card')
      .findDcy('language-icon-list-item')
      .should('have.length', 3);
    gcy('suggestion-author-card-profile-link').should(
      'have.attr',
      'href',
      `/users/${alice.id}`
    );
  });

  it('shows only a not-available line for an author the endpoint rejects', () => {
    hoverSuggestionAuthor(publicProjectId, 'key-3', dave.id);
    gcy('suggestion-author-card')
      .should('be.visible')
      .and('contain', dave.name);
    gcy('suggestion-author-card-unavailable').should('be.visible');
    gcy('suggestion-author-card-stats').should('not.exist');
    gcy('suggestion-author-card-profile-link').should('not.exist');
    cy.location('pathname').should('contain', '/translations');
  });

  it('keeps the plain author tooltip on a private project', () => {
    hoverSuggestionAuthor(privateProjectId, 'private-key', alice.id);
    gcy('suggestion-author-tooltip').should('be.visible');
    gcy('suggestion-author-card').should('not.exist');
  });
});
