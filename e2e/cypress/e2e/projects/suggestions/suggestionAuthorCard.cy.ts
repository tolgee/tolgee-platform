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

  function translateCellAsOwner(projectId: number, key: string) {
    login('communityStatsOwner').then(() => {
      setTranslations(projectId, key, { cs: 'Překlad' });
    });
  }

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
    translateCellAsOwner(publicProjectId, 'key-1');
    hoverSuggestionAuthor(publicProjectId, 'key-1', alice.id);
    gcy('suggestion-author-card')
      .should('be.visible')
      .and('contain', alice.name);
    const aliceCountsInSeed = { accepted: 6, suggested: 15, declined: 4 };
    const aliceContributionsInSeed = 17;
    gcy('suggestion-author-card-stats')
      .invoke('text')
      .should((text) => {
        Object.entries(aliceCountsInSeed).forEach(([name, count]) =>
          expect(text, name).to.match(new RegExp(`\\b${count}\\b`))
        );
      });
    gcy('suggestion-author-card-contributions')
      .invoke('text')
      .should('match', new RegExp(`\\b${aliceContributionsInSeed}\\b`));
    gcy('suggestion-author-card')
      .findDcy('language-icon-list-item')
      .should('have.length', 3);
    gcy('suggestion-author-card-since')
      .invoke('text')
      .should('match', /\b20\d\d\b/);
    gcy('suggestion-author-card-profile-link').should(
      'have.attr',
      'href',
      `/users/${alice.id}`
    );
  });

  it('shows only a private-profile line and stays on the page for an author the endpoint rejects', () => {
    translateCellAsOwner(publicProjectId, 'key-3');
    hoverSuggestionAuthor(publicProjectId, 'key-3', dave.id);
    gcy('suggestion-author-card')
      .should('be.visible')
      .and('contain', dave.name);
    gcy('suggestion-author-card-private').should('be.visible');
    gcy('suggestion-author-card-stats').should('not.exist');
    gcy('suggestion-author-card-contributions').should('not.exist');
    gcy('suggestion-author-card-profile-link').should('not.exist');
    cy.location('pathname').should('contain', '/translations');
  });

  it('keeps the plain author tooltip on a private project', () => {
    translateCellAsOwner(privateProjectId, 'private-key');
    hoverSuggestionAuthor(privateProjectId, 'private-key', alice.id);
    gcy('suggestion-author-tooltip').should('be.visible');
    gcy('suggestion-author-card').should('not.exist');
  });
});
