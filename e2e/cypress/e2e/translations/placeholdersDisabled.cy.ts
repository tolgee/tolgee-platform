import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import {
  deleteProject,
  login,
  createProject,
  createKey,
} from '../../common/apiCalls/common';
import { waitForGlobalLoading } from '../../common/loading';
import {
  getPluralEditor,
  getTranslationCell,
  selectLangsInLocalstorage,
  visitTranslations,
} from '../../common/translations';

describe('disabled placeholders translation plurals', () => {
  let project: ProjectDTO = null;
  beforeEach(() => {
    return login().then(() => {
      return createProject({
        name: 'Test',
        languages: [
          {
            tag: 'en',
            name: 'English',
            originalName: 'English',
          },
        ],
        icuPlaceholders: false,
      }).then((r) => {
        project = r.body as ProjectDTO;
        selectLangsInLocalstorage(project.id, ['en']);
      });
    });
  });

  afterEach(() => {
    deleteProject(project.id);
  });

  it('correctly escapes empty parameter', () => {
    testTextStaysTheSame('{}');
  });

  it('correctly escapes escape characeters', () => {
    testTextStaysTheSame("'{}'");
  });

  it('correctly escapes complicated translation', () => {
    testTextStaysTheSame("'{}'''''''''''{}'{}'{}{}'");
  });

  function testTextStaysTheSame(text: string) {
    visit();
    createKey(project.id, 'key 01', {}, { isPlural: true });
    getTranslationCell('key 01', 'en').click();
    getPluralEditor('other').type(text, {
      parseSpecialCharSequences: false,
    });
    cy.gcy('translations-cell-main-action-button').click();
    waitForGlobalLoading();
    cy.gcy('global-editor').should('not.exist');
    cy.gcy('translation-plural-variant').contains(text);
  }

  const visit = () => {
    visitTranslations(project.id);
  };
});
