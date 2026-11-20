import { login } from '../../../common/apiCalls/common';
import { communityStatsData } from '../../../common/apiCalls/testData/testData';
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
    communityStatsData.clean();
    communityStatsData.generateStandard().then((r) => {
      const byName = (name: string) =>
        r.body.projects.find((p) => p.name === name)!.id;
      publicProjectId = byName('Community Stats Demo');
      privateProjectId = byName('Community Stats Private');
      alice = r.body.users.find((u) => u.username === 'alice@community.test')!;
      dave = r.body.users.find((u) => u.username === 'dave@community.test')!;
    });
  });

  afterEach(() => {
    communityStatsData.clean();
  });

  function hoverSuggestionAuthor(
    projectId: number,
    key: string,
    authorId: number
  ) {
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
    login('communityStatsOwner');
    hoverSuggestionAuthor(publicProjectId, 'key-1', alice.id);
    gcy('suggestion-author-card')
      .should('be.visible')
      .and('contain', alice.name);
    gcy('community-contributor-card-suggestions')
      .invoke('text')
      .should('match', /\b15\b/);
    gcy('community-contributor-card-accepted')
      .invoke('text')
      .should('match', /\b6\b/);
    gcy('community-contributor-card-suggestions').trigger('mouseover');
    gcy('community-contributor-card-outcomes')
      .invoke('text')
      .should((text) => {
        expect(text, 'accepted').to.match(/(^|\D)6(\D|$)/);
        expect(text, 'declined').to.match(/(^|\D)4(\D|$)/);
      });
    const aliceContributionsInSeed = 17;
    gcy('community-contributor-card-contributions')
      .invoke('text')
      .should('match', new RegExp(`\\b${aliceContributionsInSeed}\\b`));
    gcy('community-contributor-card-flag').should('have.length', 3);
    gcy('community-contributor-card-last-active')
      .should('be.visible')
      .trigger('mouseover');
    gcy('community-contributor-card-since-date')
      .invoke('text')
      .should('match', /\b20\d\d\b/);
    gcy('community-contributor-card-profile-link').should(
      'have.attr',
      'href',
      `/users/${alice.id}`
    );
  });

  it('shows only a no-stats line and stays on the page for an author the endpoint rejects', () => {
    login('communityStatsOwner');
    hoverSuggestionAuthor(publicProjectId, 'key-3', dave.id);
    gcy('suggestion-author-card')
      .should('be.visible')
      .and('contain', dave.name);
    gcy('suggestion-author-card-no-stats').should('be.visible');
    gcy('community-contributor-card-suggestions').should('not.exist');
    gcy('community-contributor-card-contributions').should('not.exist');
    gcy('community-contributor-card-profile-link').should('not.exist');
    cy.location('pathname').should('contain', '/translations');
  });

  it('keeps the plain author tooltip on a private project', () => {
    login('communityStatsOwner');
    hoverSuggestionAuthor(privateProjectId, 'private-key', alice.id);
    gcy('suggestion-author-tooltip').should('be.visible');
    gcy('suggestion-author-card').should('not.exist');
  });
});
