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

    // Re-select that archive for import (read from the downloads folder as binary).
    // The dropzone's hidden <input type=file> is a sibling of the dropzone div,
    // so scope the lookup to the whole section rather than the dropzone element.
    gcy('project-settings-export-import')
      .find('input[type=file]')
      .selectFile(`${downloadsFolder}/${EXPORT_FILE}`, { force: true });

    // Manifest preview shows the source project name; same version => no warning.
    gcy('project-settings-import-manifest')
      .should('be.visible')
      .and('contain.text', 'Test');
    gcy('project-settings-import-version-warning').should('not.exist');

    // Destructive confirmation (hard mode: retype the project name).
    gcy('project-settings-import-button').should('not.be.disabled').click();
    confirmHardMode();

    // On success the component clears the selection, so the preview disappears.
    gcy('project-settings-import-manifest').should('not.exist');
    gcy('project-settings-import-button').should('be.disabled');
  });
});

describe('Project settings - Export & Import (non-admin gating)', () => {
  let projectId: number;

  beforeEach(() => {
    administrationTestData.clean();
    administrationTestData.generate();
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
