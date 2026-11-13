import { createKey, login, v2apiFetch } from '../../common/apiCalls/common';
import { notificationsPocTestData } from '../../common/apiCalls/testData/testData';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { gcy } from '../../common/shared';
import { HOST } from '../../common/constants';
import {
  commentMention,
  getCommentInputField,
  mentionPickerItem,
} from '../../common/comments';
import { waitForNotificationItem } from '../../common/notifications';

const KEY_NAME = 'checkout.pay_button';

type TranslationRef = { keyId: number; translationId: number };

describe('Comment mentions and grouped work notifications', () => {
  let projectId: number;
  let annaId: number;
  let andreasId: number;
  let tomId: number;
  let veraId: number;
  let helpCenterId: number;

  const userId = (data: TestDataStandardResponse, username: string) =>
    data.users.find((u) => u.username === username).id;

  const findGermanTranslation = () =>
    v2apiFetch(
      `projects/${projectId}/translations?languages=de&search=${KEY_NAME}`
    ).then((r): TranslationRef => {
      const key = r.body._embedded.keys.find((k) => k.keyName === KEY_NAME);
      return { keyId: key.keyId, translationId: key.translations.de.id };
    });

  const visitGermanComments = (keyId: number) =>
    cy.visit(
      `${HOST}/projects/${projectId}/translations/single?id=${keyId}&languages=en&languages=de&comments=de`
    );

  beforeEach(() => {
    notificationsPocTestData.clean();
    notificationsPocTestData.generateStandard().then((r) => {
      projectId = r.body.projects.find((p) => p.name === 'Mobile App').id;
      annaId = userId(r.body, 'anna@example.com');
      andreasId = userId(r.body, 'andreas@example.com');
      tomId = userId(r.body, 'tom@example.com');
      veraId = userId(r.body, 'vera@example.com');
      helpCenterId = r.body.projects.find((p) => p.name === 'Help Center').id;
    });
  });

  afterEach(() => {
    notificationsPocTestData.clean();
  });

  it('mentions a teammate who can view the language', () => {
    login('marek@example.com')
      .then(() => findGermanTranslation())
      .then(({ keyId }) => visitGermanComments(keyId));

    getCommentInputField().type('@An');

    gcy('comment-mention-picker-item').should('have.length', 2);
    mentionPickerItem(annaId).should('be.visible');
    mentionPickerItem(andreasId).should('be.visible');
    mentionPickerItem(tomId).should('not.exist');

    gcy('comment-mention-picker-item').then((items) => {
      const annaIndex = items
        .toArray()
        .findIndex((el) => el.getAttribute('data-cy-user-id') === `${annaId}`);
      if (annaIndex > 0) {
        getCommentInputField().type('{downArrow}'.repeat(annaIndex));
      }
    });
    mentionPickerItem(annaId).should('have.class', 'Mui-selected');
    getCommentInputField().type('{enter}');
    gcy('comment-mention-picker').should('not.exist');

    getCommentInputField().type('please check{enter}');

    gcy('comment')
      .last()
      .within(() => {
        commentMention(annaId).should('be.visible');
      });
  });

  it('shows the mention in the bell and links to the comment', () => {
    login('marek@example.com')
      .then(() => findGermanTranslation())
      .then(({ translationId }) =>
        v2apiFetch(
          `projects/${projectId}/translations/${translationId}/comments`,
          {
            method: 'POST',
            body: {
              text: `<@${annaId}> please check`,
              state: 'NEEDS_RESOLUTION',
            },
          }
        )
      );

    login('anna@example.com');
    cy.visit(HOST);
    gcy('notifications-button').click();
    gcy('notifications-comment-mention-item').should('have.length', 1).click();

    cy.location('pathname').should(
      'eq',
      `/projects/${projectId}/translations/single`
    );
    gcy('comment')
      .last()
      .within(() => {
        commentMention(annaId).should('be.visible');
      });
  });

  it('groups added keys into one notification that prefilters the list', () => {
    login('marek@example.com').then(() => {
      createKey(projectId, 'onboarding.welcome', { en: 'Welcome' });
      createKey(projectId, 'onboarding.skip', { en: 'Skip' });
    });

    login('anna@example.com');
    cy.visit(HOST);
    gcy('notifications-button').click();
    waitForNotificationItem('notifications-localization-item');
    gcy('notifications-keys-added-sample').should('have.length', 2);
    gcy('notifications-localization-item').click();

    gcy('translations-notification-prefilter-detail').should('be.visible');
    gcy('translations-row').should('have.length', 2);
    gcy('translations-key-name').should('contain', 'onboarding.welcome');
    gcy('translations-key-name').should('contain', 'onboarding.skip');
  });
  it('offers an earlier commenter first and notifies them on a public project', () => {
    login('marek@example.com')
      .then(() =>
        v2apiFetch(
          `projects/${helpCenterId}/translations?languages=de&search=faq.refund_policy`
        )
      )
      .then((r) => {
        const key = r.body._embedded.keys.find(
          (k) => k.keyName === 'faq.refund_policy'
        );
        cy.visit(
          `${HOST}/projects/${helpCenterId}/translations/single?id=${key.keyId}&languages=en&languages=de&comments=de`
        );
      });

    getCommentInputField().type('@');
    gcy('comment-mention-picker-item')
      .first()
      .should('have.attr', 'data-cy-user-id', `${veraId}`)
      .within(() => {
        gcy('comment-mention-picker-in-thread').should('be.visible');
      });
    getCommentInputField().type('{enter}');
    getCommentInputField().type('good point{enter}');
    gcy('comment')
      .last()
      .within(() => {
        commentMention(veraId).should('be.visible');
      });

    login('vera@example.com');
    cy.visit(HOST);
    gcy('notifications-button').click();
    gcy('notifications-comment-mention-item').should('have.length', 1).click();
    cy.location('pathname').should(
      'eq',
      `/projects/${helpCenterId}/translations/single`
    );
    gcy('comment')
      .last()
      .within(() => {
        commentMention(veraId).should('be.visible');
      });
  });
});
