/// <reference types="cypress" />

import { login } from '../../common/apiCalls/common';
import { administrationTestData } from '../../common/apiCalls/testData/testData';
import { HOST } from '../../common/constants';
import { gcy } from '../../common/shared';

describe('Administration apps', () => {
  beforeEach(() => {
    administrationTestData.clean();
    administrationTestData.generateStandard();
    login('admin@admin.com');
  });

  afterEach(() => {
    administrationTestData.clean();
  });

  it('opens the apps administration page', () => {
    cy.visit(`${HOST}/administration/apps`);
    gcy('global-paginated-list').should('be.visible');
    gcy('administration-apps-list-item').should('not.exist');
  });
});
