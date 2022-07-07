import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import { gcy, selectInSelect } from '../../common/shared';
import { ApiKeyDTO } from '../../../../webapp/src/service/response.types';
import {
  createApiKey,
  createTestProject,
  deleteAllProjectApiKeys,
  login,
} from '../../common/apiCalls/common';

describe('Integrate view', () => {
  let projectId: number;

  const loginAndCreateProject = () =>
    login().then(() =>
      createTestProject().then((p) => {
        projectId = p.body.id;
      })
    );

  const loginAndCreateProjectAndVisit = () =>
    loginAndCreateProject().then(() => {
      cy.visit(`${HOST}/projects/${projectId}/integrate`);
    });

  it('gets to integrate view', () => {
    loginAndCreateProjectAndVisit();
    cy.gcy('integrate-navigation-title').should('be.visible');
  });

  describe('Step 1 | Choose your weapon', () => {
    beforeEach(() => {
      loginAndCreateProjectAndVisit();
    });

    it('has visible label', () => {
      cy.gcy('integrate-choose-your-weapon-step-label').should('be.visible');
    });

    it('has visible content', () => {
      cy.gcy('integrate-choose-your-weapon-step-content').should('be.visible');
    });

    it('contains all weapons', () => {
      cy.gcy('integrate-choose-your-weapon-step-content')
        .should('contain', 'React')
        .should('contain', 'Angular')
        .should('contain', 'Next.js')
        .should('contain', 'Gatsby')
        .should('contain', 'Php')
        .should('contain', 'Rest')
        .should('contain', 'Web')
        .should('contain', 'JS (NPM)');
    });

    it('weapon can be selected', () => {
      cy.gcy('integrate-choose-your-weapon-step-content')
        .contains('React')
        .click();
      cy.gcy('integrate-select-api-key-step-content').should('be.visible');
    });

    it('weapon is stored, so its selected after refresh', () => {
      cy.gcy('integrate-choose-your-weapon-step-content')
        .contains('React')
        .click();
      cy.reload();
      cy.gcy('integrate-select-api-key-step-content').should('be.visible');
    });
  });

  describe('Step 2 | Select API key', () => {
    beforeEach(() => {
      loginAndCreateProjectAndVisit();
      cy.gcy('integrate-choose-your-weapon-step-content')
        .contains('Angular')
        .click();
    });

    it('has visible label', () => {
      cy.gcy('integrate-select-api-key-step-label').should('be.visible');
    });

    it('has visible content', () => {
      cy.gcy('integrate-select-api-key-step-content').should('be.visible');
    });

    it('contains the selector', () => {
      cy.gcy('integrate-api-key-selector-select').should('be.visible');
    });

    describe('new api key', () => {
      beforeEach(() => {
        deleteAllProjectApiKeys(projectId);
      });

      it('can create new API key when no API key exists', () => {
        createNewApiKey();
        getApiKeySelectValue().should('gt', 1000000);
      });
    });

    it('can create new API key when some API keys exist', () => {
      createApiKeysAndSelectOne(projectId).then(() => {
        cy.intercept('POST', '/v2/api-keys').as('create');
        createNewApiKey();
        cy.wait('@create').then((i) => {
          const created = i.response.body;
          gcy('integrate-api-key-selector-select').contains(created.key);
          getApiKeySelectValue().then(() => {
            cy.wrap(created).its('id').should('eq', created.id);
          });
        });
      });
    });

    describe('existing API key', () => {
      let created: ApiKeyDTO;
      beforeEach(() => {
        createApiKeysAndSelectOne(projectId).then((v) => {
          created = v;
        });
      });

      it('can use existing API key', () => {
        getApiKeySelectValue().should('eq', created.id);
      });

      it('key is stored in local storage', () => {
        cy.reload();
        getApiKeySelectValue().should('eq', created.id);
      });
    });

    afterEach(() => {
      deleteAllProjectApiKeys(projectId);
    });
  });

  describe('Step 3 | Guides work', () => {
    before(() => {
      loginAndCreateProjectAndVisit().then(() => {
        gcy('integrate-weapon-selector-button').contains('React').click();
        createApiKeysAndSelectOne(projectId).then((k) => {
          cy.wait(200);
          gcy('integrate-guide').contains(k.key);
        });
      });
    });

    beforeEach(() => {
      login();
    });

    const data: {
      weapon: string;
      textsToContain: string[];
    }[] = [
      {
        weapon: 'React',
        textsToContain: ['@tolgee/react', 'TolgeeProvider', 'useTranslate'],
      },
      {
        weapon: 'Angular',
        textsToContain: ['@tolgee/ngx', "'hello_world' | translate"],
      },
      {
        weapon: 'Next.js',
        textsToContain: ['@tolgee/react'],
      },
      {
        weapon: 'Gatsby',
        textsToContain: ['@tolgee/react', 'gatsby-plugin-react-intl'],
      },
      {
        weapon: 'Php',
        textsToContain: ['composer require tolgee'],
      },
      {
        weapon: 'Web',
        textsToContain: [
          'https://unpkg.com/@tolgee/core/dist/tolgee.umd.js',
          'https://unpkg.com/@tolgee/ui/dist/tolgee-ui.umd.js',
        ],
      },
      {
        weapon: 'JS',
        textsToContain: ['npm install @tolgee/core @tolgee/ui'],
      },
      {
        weapon: 'Rest',
        textsToContain: ['http', '/api/project/export/jsonZip'],
      },
    ];

    data.forEach((item) => {
      it(`${item.weapon} works`, () => {
        gcy('integrate-weapon-selector-button').contains(item.weapon).click();
        item.textsToContain.forEach((t) => {
          gcy('integrate-guide').contains(t);
        });
        cy.contains('Go to Docs')
          .closest('a')
          .then(($a) => {
            expect($a[0].href).contains('https://tolgee.io');
          });
      });
    });
  });
});

const getApiKeySelectValue = () => {
  return cy
    .gcy('integrate-api-key-selector-select-input')
    .invoke('val')
    .should('not.be.empty')
    .then((v) => parseInt(v as string));
};

const createApiKeysAndSelectOne = (projectId: number): Promise<ApiKeyDTO> => {
  createApiKey({ projectId: projectId, scopes: ['translations.edit'] });
  return createApiKey({
    projectId: projectId,
    scopes: ['translations.edit'],
  }).then((v) =>
    cy
      .reload()
      .then(() =>
        selectInSelect(cy.gcy('integrate-api-key-selector-select'), v.key).then(
          () => v
        )
      )
  ) as Promise<ApiKeyDTO>;
};

const createNewApiKey = () => {
  cy.gcy('integrate-api-key-selector-select').click();
  cy.gcy('integrate-api-key-selector-create-new-item').click();
  cy.gcy('global-form-save-button').click();
};
