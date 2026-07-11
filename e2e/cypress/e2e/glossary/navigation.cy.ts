import {
  getOrganizationByNameFromTestData,
  glossaryTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { HOST } from '../../common/constants';
import { E2GlossariesView } from '../../compounds/glossaries/E2GlossariesView';

describe('Glossaries navigation', () => {
  let data: TestDataStandardResponse;

  beforeEach(() => {
    glossaryTestData.clean();
    glossaryTestData.generateStandard().then((res) => {
      data = res.body;
    });
  });

  afterEach(() => {
    glossaryTestData.clean();
  });

  it('Owner can navigate from organization to glossary', () => {
    login('Owner');
    testItself();
  });

  it('Member can navigate from organization to glossary', () => {
    login('Member');
    testItself();
  });

  const testItself = () => {
    const slug = getOrganizationByNameFromTestData(data, 'Owner').slug;
    cy.visit(`${HOST}/organizations/${slug}/profile`);
    gcy('settings-menu-item').filter(':contains("Glossaries")').click();

    const glossariesView = new E2GlossariesView();
    const glossaryView = glossariesView.openGlossary('Test Glossary');

    glossaryView.checkTranslationExists('A.B.C, s.r.o.');
  };
});
