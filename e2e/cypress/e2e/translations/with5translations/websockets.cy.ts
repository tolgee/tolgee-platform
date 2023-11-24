import { visitTranslations } from '../../../common/translations';
import { waitForGlobalLoading } from '../../../common/loading';
import { login, v2apiFetch } from '../../../common/apiCalls/common';
import { translationWebsocketsTestData } from '../../../common/apiCalls/testData/testData';

describe('Translations Websockets', () => {
  let projectId: number = null;
  let keyId: number = null;
  beforeEach(() => {
    translationWebsocketsTestData.clean();
    translationWebsocketsTestData.generate().then((r) => {
      projectId = r.body.projectId;
      keyId = r.body.keyId;
    });
    login('franta');
  });

  afterEach(() => {
    translationWebsocketsTestData.clean();
  });

  it('reacts to websocket events', () => {
    visit();
    waitForGlobalLoading();
    cy.contains('Z translation').should('be.visible');
    const updatedValue = 'I am updated!';
    v2apiFetch(`projects/${projectId}/translations`, {
      method: 'put',
      body: { key: 'A key', translations: { de: updatedValue } },
    });
    cy.contains(updatedValue).should('be.visible');
    cy.waitForDom();
    v2apiFetch(`projects/${projectId}/keys/${keyId}`, {
      method: 'delete',
    });
    cy.waitForDom();
    cy.contains('A key')
      .closestDcy('translations-row')
      .then(($el) => {
        const className = $el.get(0).className;
        expect(className).to.contain('deleted');
      });
  });

  const visit = () => {
    visitTranslations(projectId);
  };
});
