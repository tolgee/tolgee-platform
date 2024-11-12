import { createKey, createProject, login } from './apiCalls/common';
import { HOST } from './constants';
import { ProjectDTO } from '../../../webapp/src/service/response.types';
import { dismissMenu } from './shared';
import Chainable = Cypress.Chainable;
import { components } from '../../../webapp/src/service/apiSchema.generated';

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

export function assertExportLanguagesSelected(languages: string[]) {
  cy.gcy('export-language-selector').click();

  cy.gcy('export-language-selector-item').should('be.visible');

  languages.forEach((language) => {
    cy.gcy('export-language-selector-item')
      .contains(language)
      .closestDcy('export-language-selector-item')
      .should('be.visible')
      .find('input')
      .should('be.checked');
  });
  dismissMenu();
}
export const exportSelectFormat = (format: string) => {
  cy.gcy('export-format-selector').click();
  cy.gcy('export-format-selector-item').contains(format).click();
};

export const exportSelectMessageFormat = (format: string) => {
  cy.gcy('export-message-format-selector').click();
  cy.gcy('export-message-format-selector-item').contains(format).click();
};

export const testExportFormats = (
  interceptFn: () => ReturnType<typeof cy.intercept>,
  submitFn: () => void,
  clearCheckboxesAfter: boolean,
  afterFn: (test: FormatTest) => void
) => {
  const testFormatWithMessageFormats = (
    supportedMessageFormats: SupportedMessageFormat[],
    test: FormatTest
  ) => {
    supportedMessageFormats.forEach((messageFormat) => {
      testFormat(interceptFn, submitFn, clearCheckboxesAfter, afterFn, {
        ...test,
        messageFormat,
        expectedParams: {
          ...test.expectedParams,
          messageFormat: messageFormatParamMap[messageFormat],
        },
      });
    });
  };

  testFormat(interceptFn, submitFn, clearCheckboxesAfter, afterFn, {
    format: 'JSON',
    expectedParams: {
      format: 'JSON',
      supportArrays: false,
    },
  });

  testFormat(interceptFn, submitFn, clearCheckboxesAfter, afterFn, {
    format: 'Structured JSON',
    messageFormat: 'Java String.format',
    expectedParams: {
      format: 'JSON',
      structureDelimiter: '.',
      supportArrays: false,
    },
  });

  testFormatWithMessageFormats(
    ['ICU', 'PHP Sprintf', 'C Sprintf', 'Ruby Sprintf', 'Java String.format'],
    {
      format: 'Structured JSON',
      expectedParams: {
        format: 'JSON',
        structureDelimiter: '.',
        supportArrays: false,
      },
    }
  );

  testFormatWithMessageFormats(
    ['ICU', 'PHP Sprintf', 'C Sprintf', 'Ruby Sprintf', 'Java String.format'],
    {
      format: 'XLIFF',
      expectedParams: {
        format: 'XLIFF',
      },
    }
  );

  testFormatWithMessageFormats(
    ['ICU', 'PHP Sprintf', 'C Sprintf', 'Java String.format'],
    {
      format: 'Flat YAML',
      expectedParams: {
        format: 'YAML',
      },
    }
  );

  testFormatWithMessageFormats(
    ['ICU', 'PHP Sprintf', 'C Sprintf', 'Java String.format'],
    {
      format: 'Structured YAML',
      expectedParams: {
        format: 'YAML',
      },
    }
  );

  testFormatWithMessageFormats(['ICU', 'Java String.format'], {
    format: '.properties',
    expectedParams: {
      format: 'PROPERTIES',
    },
  });

  testFormatWithMessageFormats(
    ['ICU', 'PHP Sprintf', 'C Sprintf', 'Ruby Sprintf', 'Java String.format'],
    {
      format: 'Gettext (.po)',
      expectedParams: {
        format: 'PO',
        messageFormat: 'PHP_SPRINTF',
      },
    }
  );

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

  testFormat(interceptFn, submitFn, clearCheckboxesAfter, afterFn, {
    format: 'Ruby on Rails .yaml',
    expectedParams: {
      messageFormat: 'RUBY_SPRINTF',
      format: 'YAML_RUBY',
    },
  });

  testFormat(interceptFn, submitFn, clearCheckboxesAfter, afterFn, {
    format: 'Flat i18next .json',
    expectedParams: {
      format: 'JSON_I18NEXT',
    },
  });

  testFormat(interceptFn, submitFn, clearCheckboxesAfter, afterFn, {
    format: 'Structured i18next .json',
    expectedParams: {
      format: 'JSON_I18NEXT',
      structureDelimiter: '.',
    },
  });

  testFormatWithMessageFormats(
    ['ICU', 'PHP Sprintf', 'C Sprintf', 'Ruby Sprintf', 'Java String.format'],
    {
      format: 'CSV',
      expectedParams: {
        format: 'CSV',
      },
    }
  );
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
  test.messageFormat && exportSelectMessageFormat(test.messageFormat);
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

export type FormatTest = {
  format: string;
  clickCheckboxes?: DataCy.Value[];
  messageFormat?: SupportedMessageFormat;
  expectedParams: {
    messageFormat?: components['schemas']['ExportParams']['messageFormat'];
    format: string;
    structureDelimiter?: string;
    supportArrays?: boolean;
  };
};

const messageFormatParamMap = {
  ICU: 'ICU' as MessageFormat,
  'PHP Sprintf': 'PHP_SPRINTF' as MessageFormat,
  'C Sprintf': 'C_SPRINTF' as MessageFormat,
  'Java String.format': 'JAVA_STRING_FORMAT' as MessageFormat,
  'Ruby Sprintf': 'RUBY_SPRINTF' as MessageFormat,
};

type MessageFormat = components['schemas']['ExportParams']['messageFormat'];

type SupportedMessageFormat = keyof typeof messageFormatParamMap;

export const getFileName = (
  projectName: string,
  extension: string,
  language?: string
) => {
  const dateStr = '_' + new Date().toISOString().split('T')[0];
  const languageStr = language ? `_${language}` : '';
  return `${projectName}${languageStr}${dateStr}.${extension}`;
};

type ZipContentProps = {
  path: string;
  file: string;
  filesContent: Record<string, (content: string) => void>;
};

export function checkZipContent({ path, file, filesContent }: ZipContentProps) {
  cy.task('unzipping', {
    path,
    file,
  }).then((result: any) => {
    Object.entries(filesContent).map(([name, callback]) => {
      const file = result.find((i) => i.path === name);
      callback(Buffer.from(file.data.data).toString());
    });
  });
}
