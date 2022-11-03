import { HOST } from '../../common/constants';
import { patsTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { confirmStandard, gcy, selectInSelect } from '../../common/shared';
import {
  formatCurrentDatePlusDays,
  setExpiration,
} from '../../common/apiKeysAndPats';

describe('Personal Access tokens', () => {
  beforeEach(() => {
    patsTestData.clean();
    patsTestData.generate();
    login('user@user.com');
    cy.visit(`${HOST}/account/personal-access-tokens`);
  });

  afterEach(() => {
    patsTestData.clean();
  });

  const EXPIRED_STRING = 'Token expired on Wednesday, August 24, 2022';

  it('list items rendered', () => {
    gcy('pat-list-item').should('have.length', 3);
    gcy('pat-list-item')
      .findDcy('pat-list-item-description')
      .contains('Yee2')
      .should('be.visible');
    gcy('pat-list-item')
      .findDcy('pat-expiry-info')
      .contains('Never expires')
      .should('be.visible');
    gcy('pat-list-item')
      .findDcy('pat-expiry-info')
      .contains(EXPIRED_STRING)
      .should('be.visible');
    gcy('pat-list-item').contains('Never used').should('be.visible');
    gcy('pat-list-item')
      .contains('Used on August 24, 2022')
      .should('be.visible');
  });

  it('adds token', () => {
    createAndAssertToken({
      description: 'Hello! I was just created!',
      selectValue: 'Custom date',
      customDate: '09/15/2050',
      expectFormattedDate: 'Expires on Thursday, September 15, 2050',
    });
    createAndAssertToken({
      description: 'Hello2',
      selectValue: '7 days',
      expectFormattedDate: formatCurrentDatePlusDays(7),
    });
    createAndAssertToken({
      description: 'Hello hello',
      selectValue: '30 days',
      expectFormattedDate: formatCurrentDatePlusDays(30),
    });
    createAndAssertToken({
      description: 'Hello hello',
      selectValue: 'Never expires',
      expectFormattedDate: 'Never expires',
    });
  });

  it('regenerates token', () => {
    cy.contains(EXPIRED_STRING)
      .closestDcy('pat-list-item')
      .findDcy('pat-list-item-regenerate-button')
      .click();
    selectInSelect(gcy('expiration-select'), '30 days');
    gcy('global-form-save-button').click();
    cy.contains(EXPIRED_STRING).should('not.exist');
  });

  it('deletes token', () => {
    cy.contains(EXPIRED_STRING)
      .closestDcy('pat-list-item')
      .findDcy('pat-list-item-delete-button')
      .click();
    confirmStandard();
    cy.contains(EXPIRED_STRING).should('not.exist');
  });

  it('edits token', () => {
    cy.contains(EXPIRED_STRING)
      .closestDcy('pat-list-item')
      .findDcy('pat-list-item-description')
      .click();
    const text = 'I am updated now.';
    gcy('edit-pat-dialog-description-input').clear().type(text);
    gcy('global-form-save-button').click();
    cy.contains(EXPIRED_STRING).closestDcy('pat-list-item').contains(text);
  });
});

const createAndAssertToken = ({
  description,
  selectValue,
  customDate,
  expectFormattedDate,
}: {
  description: string;
  selectValue: string;
  customDate?: string;
  expectFormattedDate: string;
}) => {
  gcy('global-plus-button').click();
  gcy('generate-pat-dialog-title').should('be.visible');
  gcy('generate-pat-dialog-description-input').type(description);
  setExpiration(selectValue, customDate);
  gcy('global-form-save-button').click();
  cy.contains(description).should('be.visible');
  gcy('pat-list-item-new-token-input')
    .find('input')
    .should('include.value', 'tgpat_');
  gcy('pat-list-item-alert').contains('Token created.');
  cy.waitForDom();
  gcy('pat-list-item').first().contains(expectFormattedDate);
  gcy('pat-list-item').first().contains('Never used');
};
