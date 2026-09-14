import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import {
  createProject,
  deleteAllEmails,
  disableEmailVerification,
  getParsedEmailInvitationLink,
  login,
} from '../../common/apiCalls/common';
import { HOST } from '../../common/constants';
import { assertMessage, selectInProjectMenu } from '../../common/shared';
import { Clipboard, stubClipboard } from '../../common/clipboard';

let project: ProjectDTO;
let clipboard: Clipboard;

describe('Project Invitation', () => {
  beforeEach(() => {
    login().then(() => {
      createProject({
        name: 'Test',
        languages: [
          {
            tag: 'en',
            name: 'English',
            originalName: 'English',
            flagEmoji: '🇬🇧',
          },
        ],
      })
        .then((r) => (project = r.body))
        .then(() => {
          cy.visit(`${HOST}/projects/${project.id}`);
        });
    });
    disableEmailVerification();
  });

  afterEach(() => {
    deleteAllEmails();
  });

  it('sends invitation code via email', () => {
    cy.visit(`${HOST}/projects/${project.id}`);
    selectInProjectMenu('Members');
    cy.gcy('invite-generate-button').click();
    cy.gcy('invitation-dialog-input-field').type('test@invitation.com');
    cy.gcy('invitation-dialog-invite-button').click();

    assertMessage('Invitation was sent');
    getParsedEmailInvitationLink().then((link) => {
      expect(link).to.have.length.greaterThan(50);
      expect(link).to.contain('/accept_invitation/');
    });
  });

  it('copies invitation code to clipboard', () => {
    cy.visit(`${HOST}/projects/${project.id}`, {
      onBeforeLoad(win) {
        clipboard = stubClipboard(win);
      },
    });
    selectInProjectMenu('Members');
    cy.gcy('invite-generate-button').click();
    cy.gcy('invitation-dialog-type-link-button').click();
    cy.gcy('invitation-dialog-input-field').type('test@invitation.com');
    cy.gcy('invitation-dialog-invite-button').click();
    assertMessage('Invitation link copied to clipboard');
    cy.wrap(() => {
      expect(clipboard.text).to.have.length.greaterThan(50);
      expect(clipboard.text).to.contain('/accept_invitation/');
    });
  });
});
