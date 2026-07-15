/// <reference types="cypress" />

import 'cypress-file-upload';
import {
  createTestProject,
  deleteProject,
  login,
} from '../../common/apiCalls/common';
import { administrationTestData } from '../../common/apiCalls/testData/testData';
import {
  confirmHardMode,
  gcy,
  visitProjectSettings,
} from '../../common/shared';

// The exported archive's filename comes from the backend Content-Disposition
// (`<projectName>.zip`); the project created by createTestProject is named "Test".
const EXPORT_FILE = 'Test.zip';

describe('Project settings - Export & Import (server admin)', () => {
  const downloadsFolder = Cypress.config('downloadsFolder');
  let projectId: number;

  beforeEach(() => {
    // The download name is a fixed constant; drop any stale copy so verifyDownload
    // can't pass on a leftover archive from a previous test.
    cy.exec(`rm -f "${downloadsFolder}/${EXPORT_FILE}"`, {
      failOnNonZeroExit: false,
    });
    login().then(() =>
      createTestProject().then((r) => {
        projectId = r.body.id;
      })
    );
  });

  afterEach(() => {
    deleteProject(projectId);
  });

  it('shows the Export & Import tab to a server admin', () => {
    visitProjectSettings(projectId);
    gcy('project-settings-menu-export-import').should('be.visible').click();
    gcy('project-settings-export-import').should('be.visible');
  });

  it('exports the project as a zip', () => {
    visitProjectSettings(projectId);
    gcy('project-settings-menu-export-import').click();
    gcy('project-settings-export-button').click();
    cy.verifyDownload(EXPORT_FILE);
  });

  it('previews the manifest and imports an exported archive (round-trip)', () => {
    visitProjectSettings(projectId);
    gcy('project-settings-menu-export-import').click();

    // Export first to obtain an archive guaranteed to match the running version.
    gcy('project-settings-export-button').click();
    cy.verifyDownload(EXPORT_FILE);

    // The dropzone's hidden <input type=file> is a sibling of the dropzone div,
    // so scope the lookup to the whole section rather than the dropzone element.
    gcy('project-settings-export-import')
      .find('[data-cy=file-dropzone-file-input]')
      .selectFile(`${downloadsFolder}/${EXPORT_FILE}`, { force: true });

    gcy('project-settings-import-manifest')
      .should('be.visible')
      .and('contain.text', 'Test');
    gcy('project-settings-import-version-warning').should('not.exist');

    gcy('project-settings-import-button').should('not.be.disabled').click();
    confirmHardMode();

    gcy('project-settings-import-manifest').should('not.exist');
    gcy('project-settings-import-button').should('be.disabled');
  });

  it('rejects an unreadable file and keeps the import button disabled', () => {
    visitProjectSettings(projectId);
    gcy('project-settings-menu-export-import').click();

    gcy('project-settings-export-import')
      .find('[data-cy=file-dropzone-file-input]')
      .selectFile(
        {
          contents: Cypress.Buffer.from('not a real zip archive'),
          fileName: 'broken.zip',
          mimeType: 'application/zip',
        },
        { force: true }
      );

    gcy('project-settings-import-manifest-unreadable').should('be.visible');
    gcy('project-settings-import-manifest').should('not.exist');
    gcy('project-settings-import-version-warning').should('not.exist');
    gcy('project-settings-import-button').should('be.disabled');
  });

  it('imports a version-mismatched archive via the override confirmation', () => {
    cy.intercept('POST', '**/administration/projects/*/import*', {
      statusCode: 200,
      body: {},
    }).as('import');

    visitProjectSettings(projectId);
    gcy('project-settings-menu-export-import').click();

    gcy('project-settings-export-import')
      .find('[data-cy=file-dropzone-file-input]')
      .selectFile('cypress/fixtures/version-mismatch-export.zip', {
        force: true,
      });

    gcy('project-settings-import-manifest').should('be.visible');
    gcy('project-settings-import-version-warning').should('be.visible');

    gcy('project-settings-import-button').should('not.be.disabled').click();
    cy.contains('0.0.0-mismatch').should('be.visible');
    cy.contains('wipes project Test').should('be.visible');
    confirmHardMode();

    cy.wait('@import')
      .its('request.url')
      .should('include', 'ignoreVersion=true');
    gcy('project-settings-import-manifest').should('not.exist');
  });
});

describe('Project settings - Export & Import (non-admin gating)', () => {
  let projectId: number;

  beforeEach(() => {
    administrationTestData.clean();
    administrationTestData.generateStandard();
    login('user@user.com').then(() =>
      createTestProject().then((r) => {
        projectId = r.body.id;
      })
    );
  });

  afterEach(() => {
    deleteProject(projectId);
    administrationTestData.clean();
  });

  it('hides the Export & Import tab from a non-admin', () => {
    visitProjectSettings(projectId);
    gcy('project-settings-menu-general').should('be.visible');
    gcy('project-settings-menu-export-import').should('not.exist');
  });
});
