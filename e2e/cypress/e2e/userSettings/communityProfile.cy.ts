import { HOST } from '../../common/constants';
import { login } from '../../common/apiCalls/common';
import { communityStatsData } from '../../common/apiCalls/testData/testData';
import { gcy } from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';

const ALICE = 'alice@community.test';
const BOB = 'bob@community.test';
const CAROL = 'carol@community.test';

const project = { id: 1, name: 'Community Stats Demo' };
const keyRelation = {
  entityClass: 'Key',
  entityId: 1,
  data: { name: 'key-1' },
  relations: {},
  exists: true,
};
const languageRelation = {
  entityClass: 'Language',
  entityId: 1,
  data: { tag: 'cs', name: 'Czech', flagEmoji: '🇨🇿' },
  relations: {},
  exists: true,
};
const translationEdit = {
  revisionId: 1,
  timestamp: Date.now(),
  type: 'SET_TRANSLATIONS',
  project,
  modifiedEntities: {
    Translation: [
      {
        entityClass: 'Translation',
        entityId: 1,
        modifications: { text: { old: 'old', new: 'edited cs text' } },
        relations: { key: keyRelation, language: languageRelation },
      },
    ],
  },
};
const commentEdit = {
  revisionId: 2,
  timestamp: Date.now(),
  type: 'TRANSLATION_COMMENT_EDIT',
  project,
  modifiedEntities: {
    TranslationComment: [
      {
        entityClass: 'TranslationComment',
        entityId: 1,
        modifications: { text: { old: 'old', new: 'edited comment' } },
        relations: {
          language: languageRelation,
          translation: {
            entityClass: 'Translation',
            entityId: 1,
            data: {},
            relations: { key: keyRelation, language: languageRelation },
            exists: true,
          },
        },
      },
    ],
  },
};

describe('Community profile', () => {
  let aliceId: number;
  let carolId: number;

  beforeEach(() => {
    communityStatsData.clean();
    communityStatsData.generateStandard().then((res) => {
      aliceId = res.body.users.find((u) => u.username === ALICE).id;
      carolId = res.body.users.find((u) => u.username === CAROL).id;
    });
  });

  afterEach(() => {
    communityStatsData.clean();
  });

  const visitProfile = (userId: number) => {
    cy.visit(`${HOST}/users/${userId}`);
    waitForGlobalLoading();
  };

  it("shows another contributor's profile", () => {
    login(BOB, 'admin');
    visitProfile(aliceId);
    gcy('community-profile-identity').should('contain', 'Alice Adams');
    gcy('community-profile-totals').should('be.visible');
    gcy('community-profile-calendar').should('be.visible');
    gcy('community-profile-project').should('contain', 'Community Stats Demo');
    gcy('community-profile-activity-item').should('have.length.at.least', 1);
    gcy('community-profile-avatar-edit').should('not.exist');
  });

  it('lets the owner edit their own profile', () => {
    login(ALICE, 'admin');
    visitProfile(aliceId);
    gcy('community-profile-identity').should('contain', 'Alice Adams');
    gcy('community-profile-avatar-edit').should('exist');
    gcy('community-profile-private').should('not.exist');
  });

  it('renders translation edits and comment edits in the feed', () => {
    login(BOB, 'admin');
    cy.intercept('GET', '**/community-activity*', {
      body: {
        _embedded: { activities: [translationEdit, commentEdit] },
        page: { size: 20, totalElements: 2, totalPages: 1, number: 0 },
      },
    });
    visitProfile(aliceId);
    gcy('community-profile-activity-item').should('have.length', 2);
    gcy('community-profile-activity-text')
      .first()
      .should('contain', 'edited cs text');
    gcy('community-profile-activity-text')
      .last()
      .should('contain', 'edited comment');
  });

  it('shows an unavailable state for an unknown user', () => {
    login(BOB, 'admin');
    visitProfile(999999999);
    gcy('community-profile-unavailable').should('be.visible');
  });

  it('shows an unavailable state for a user without public contributions', () => {
    login(BOB, 'admin');
    visitProfile(carolId);
    gcy('community-profile-unavailable').should('be.visible');
  });
});
