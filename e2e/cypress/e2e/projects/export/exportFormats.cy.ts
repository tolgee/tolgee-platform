import 'cypress-file-upload';
import {
  createKey,
  deleteProject,
  login,
} from '../../../common/apiCalls/common';
import {
  createExportableProject,
  testExportFormats,
  visitExport,
} from '../../../common/export';

describe('Export Formats', () => {
  let projectId: number;
  before(() => {
    createExportableProject().then((p) => {
      createKey(p.id, `test.test`, {
        en: `Test english`,
        cs: `Test czech`,
      });
      createKey(p.id, `test.array[0]`, {
        en: `Test english`,
        cs: `Test czech`,
      });
      visitExport(p.id);
      projectId = p.id;
      cy.gcy('export-submit-button').should('be.visible');
    });
  });

  beforeEach(() => {
    login();
    visitExport(projectId);
  });

  it('correctly exports to all formats', () => {
    const submitFn = () => {
      cy.gcy('export-submit-button').click();
    };

    testExportFormats(
      () => cy.intercept('POST', '/v2/projects/*/export'),
      submitFn,
      true,
      () => {}
    );
  });

  after(() => {
    deleteProject(projectId);
  });
});
