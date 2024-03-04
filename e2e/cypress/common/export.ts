import { createKey, createProject, login } from './apiCalls/common';
import { HOST } from './constants';
import { ProjectDTO } from '../../../webapp/src/service/response.types';
import { dismissMenu } from './shared';
import Chainable = Cypress.Chainable;

export function createExportableProject(): Chainable<ProjectDTO> {
  return login().then(() => {
    return createProject({
      name: 'Test project',
      languages: [
        {
          tag: 'en',
          name: 'English',
          originalName: 'English',
        },
        {
          tag: 'cs',
          name: 'Česky',
          originalName: 'česky',
        },
      ],
    }).then((r) => {
      return r.body as ProjectDTO;
    });
  });
}

export const visitExport = (projectId: number) => {
  return cy.visit(`${HOST}/projects/${projectId}/export`);
};

export const create4Translations = (projectId: number) => {
  const promises = [];
  for (let i = 1; i < 5; i++) {
    promises.push(
      createKey(projectId, `Cool key ${i.toString().padStart(2, '0')}`, {
        en: `Cool translated text ${i}`,
        cs: `Studený přeložený text ${i}`,
      })
    );
  }
};

export const exportToggleLanguage = (lang: string) => {
  cy.gcy('export-language-selector').click();
  cy.gcy('export-language-selector-item').contains(lang).click();
  dismissMenu();
};

export const exportSelectFormat = (format: string) => {
  cy.gcy('export-format-selector').click();
  cy.gcy('export-format-selector-item').contains(format).click();
};

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

function clickCheckboxes(test: FormatTest) {
  if (test.clickCheckboxes) {
    test.clickCheckboxes.forEach((checkbox) => {
      cy.gcy(checkbox).click();
    });
  }
}

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
