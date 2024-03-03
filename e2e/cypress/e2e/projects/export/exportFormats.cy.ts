import 'cypress-file-upload';
import {
  createKey,
  deleteProject,
  login,
} from '../../../common/apiCalls/common';
import {
  createExportableProject,
  exportSelectFormat,
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

export const testExportFormats = (
  interceptFn: () => ReturnType<typeof cy.intercept>,
  submitFn: () => void,
  clearCheckboxesAfter: boolean,
  afterFn: (test: FormatTest) => void
) => {
  testFormat(interceptFn, submitFn, clearCheckboxesAfter, afterFn, {
    format: 'JSON',
    expectedParams: {
      format: 'JSON',
      supportArrays: false,
    },
  });

  testFormat(interceptFn, submitFn, clearCheckboxesAfter, afterFn, {
    format: 'Structured JSON',
    expectedParams: {
      format: 'JSON',
      structureDelimiter: '.',
      supportArrays: false,
    },
  });

  testFormat(interceptFn, submitFn, clearCheckboxesAfter, afterFn, {
    format: 'Structured JSON',
    clickCheckboxes: ['export-support_arrays-selector'] as DataCy.Value[],
    expectedParams: {
      format: 'JSON',
      structureDelimiter: '.',
      supportArrays: true,
    },
  });

  testFormat(interceptFn, submitFn, clearCheckboxesAfter, afterFn, {
    format: 'XLIFF',
    expectedParams: {
      format: 'XLIFF',
    },
  });

  testFormat(interceptFn, submitFn, clearCheckboxesAfter, afterFn, {
    format: '.properties',
    expectedParams: {
      format: 'PROPERTIES',
    },
  });

  testFormat(interceptFn, submitFn, clearCheckboxesAfter, afterFn, {
    format: 'PHP .po',
    expectedParams: {
      format: 'PO',
      messageFormat: 'PHP_SPRINTF',
    },
  });

  testFormat(interceptFn, submitFn, clearCheckboxesAfter, afterFn, {
    format: 'Python .po',
    expectedParams: {
      format: 'PO',
      messageFormat: 'PYTHON_SPRINTF',
    },
  });

  testFormat(interceptFn, submitFn, clearCheckboxesAfter, afterFn, {
    format: 'C/C++ .po',
    expectedParams: {
      format: 'PO',
      messageFormat: 'C_SPRINTF',
    },
  });

  testFormat(interceptFn, submitFn, clearCheckboxesAfter, afterFn, {
    format: 'Apple .strings & .stringsdict',
    expectedParams: {
      format: 'APPLE_STRINGS_STRINGSDICT',
    },
  });

  testFormat(interceptFn, submitFn, clearCheckboxesAfter, afterFn, {
    format: 'Apple .xliff',
    expectedParams: {
      format: 'APPLE_XLIFF',
    },
  });

  testFormat(interceptFn, submitFn, clearCheckboxesAfter, afterFn, {
    format: 'Android .xml',
    expectedParams: {
      format: 'ANDROID_XML',
    },
  });

  testFormat(interceptFn, submitFn, clearCheckboxesAfter, afterFn, {
    format: 'Flutter .arb',
    expectedParams: {
      format: 'FLUTTER_ARB',
    },
  });
};

function clickCheckboxes(test: FormatTest) {
  if (test.clickCheckboxes) {
    test.clickCheckboxes.forEach((checkbox) => {
      cy.gcy(checkbox).click();
    });
  }
}

const testFormat = (
  interceptFn: () => ReturnType<typeof cy.intercept>,
  submitFn: () => void,
  clearCheckboxesAfter: boolean,
  afterFn: (test: FormatTest) => void,
  test: FormatTest
) => {
  cy.log(`Testing format: ${test.format}`);
  const paramsJson = JSON.stringify(test);
  const alias = `exportFormRequest_${paramsJson}`;
  interceptFn().as(alias);
  exportSelectFormat(test.format);
  clickCheckboxes(test);
  submitFn();
  cy.wait(`@${alias}`).then((interception) => {
    expect(interception.request.body).to.include(test.expectedParams);
  });
  if (clearCheckboxesAfter) {
    clickCheckboxes(test);
  }
  afterFn(test);
};

type FormatTest = {
  format: string;
  clickCheckboxes?: DataCy.Value[];
  expectedParams: {
    messageFormat?: string;
    format: string;
    structureDelimiter?: string;
    supportArrays?: boolean;
  };
};
