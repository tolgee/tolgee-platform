/// <reference types="cypress" />

import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { billingAdminInvoicesTestData } from '../../common/apiCalls/testData/testData';
import { HOST } from '../../common/constants';

describe('Administration Invoices', () => {
  beforeEach(() => {
    billingAdminInvoicesTestData.clean();
    billingAdminInvoicesTestData.generate();
    login('admin-invoices-admin');
    cy.visit(`${HOST}/administration/invoices`);
  });

  afterEach(() => {
    billingAdminInvoicesTestData.clean();
  });

  it('shows the invoices section', () => {
    gcy('admin-invoices-org-section').should('be.visible');
    gcy('admin-invoices-item').should('have.length.at.least', 3);
  });

  it('shows invoice numbers and totals', () => {
    gcy('admin-invoices-item-number').should('contain', 'TEST-0001');
    gcy('admin-invoices-item-number').should('contain', 'TEST-0002');
    gcy('admin-invoices-item-number').should('contain', 'TEST-0003');
  });

  it('filters invoices by organization', () => {
    gcy('admin-invoices-org-filter').click();
    gcy('admin-invoices-org-filter').find('input').type('Second Org');
    cy.contains('li', 'Second Org').click();

    gcy('admin-invoices-item').should('have.length', 1);
    gcy('admin-invoices-item-number').should('contain', 'TEST-0003');
  });

  it('shows carry-overs section with active tab', () => {
    gcy('admin-carry-overs-section').should('be.visible');
    gcy('admin-carry-overs-tab-active').should(
      'have.attr',
      'aria-selected',
      'true'
    );
    gcy('admin-carry-over-item').should('have.length', 1);
    gcy('admin-carry-over-total').should('be.visible');
  });

  it('switches to carry-over history tab', () => {
    gcy('admin-carry-overs-tab-history').click();
    gcy('admin-carry-over-item').should('have.length', 1);
    gcy('admin-carry-over-settled-by').should('be.visible');
  });
});
