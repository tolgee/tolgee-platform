import { HOST } from '../../common/constants';
import { login } from '../../common/apiCalls/common';
import { scopedSearch } from '../../common/apiCalls/testData/testData';
import { visitTranslations } from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';

describe('Scoped search', () => {
  let projectId: number;
  const view = new E2TranslationsView();

  beforeEach(() => {
    scopedSearch.clean({ failOnStatusCode: false });
    scopedSearch
      .generateStandard()
      .then((r) => r.body)
      .then(({ users, projects }) => {
        login(users[0].username);
        projectId = projects[0].id;
        visitTranslations(projectId);
      });
    waitForGlobalLoading();
  });

  afterEach(() => {
    scopedSearch.clean({ failOnStatusCode: false });
  });

  it('searches only in key descriptions', () => {
    view.searchFor('description:cart');
    // cart.title's description is "shopping cart heading"
    view.getKeyCell('cart.title').should('be.visible');
    // "cart" is in these keys' names, not their descriptions
    view.getKeyCell('my.cart').should('not.exist');
    view.getKeyCell('cart_subtitle').should('not.exist');
  });

  it('ignores a qualifier while its value is still empty', () => {
    view.searchFor('description:');
    waitForGlobalLoading();
    view.getKeyCell('cart.title').should('be.visible');
    view.getKeyCell('checkout.title').should('be.visible');
  });

  it('excludes keys with a negated qualifier', () => {
    view.searchFor('-key:cart*');
    view.getKeyCell('my.cart').should('be.visible');
    view.getKeyCell('cart.title').should('not.exist');
  });

  it('searches keys by wildcard patterns', () => {
    view.searchFor('key:cart*');
    view.getKeyCell('cart.title').should('be.visible');
    view.getKeyCell('my.cart').should('not.exist');

    view.getSearchField().clear().type('key:*cart');
    view.getKeyCell('my.cart').should('be.visible');
    view.getKeyCell('cart.title').should('not.exist');
  });

  it('searches in a specific language', () => {
    // "Warenkorb" is only in the German translation, so scoping to de matches
    view.searchFor('de:Warenkorb');
    view.getKeyCell('cart.title').should('be.visible');
    view.getKeyCell('cart_subtitle').should('not.exist');

    // the same term scoped to en matches nothing (en is "Add to cart")
    view.getSearchField().clear().type('en:Warenkorb');
    view.getKeyCell('cart.title').should('not.exist');
  });

  it('applies a language-scoped search from a deep link on fresh load', () => {
    const search = encodeURIComponent('de:Warenkorb');
    cy.visit(`${HOST}/projects/${projectId}/translations?search=${search}`);
    view.getKeyCell('cart.title').should('be.visible');
    view.getKeyCell('cart_subtitle').should('not.exist');
  });

  it('keeps plain search behavior', () => {
    view.searchFor('Checkout');
    view.getKeyCell('checkout.title').should('be.visible');
    view.getKeyCell('cart.title').should('not.exist');
  });

  it('keeps the field single-line on Enter', () => {
    view.searchFor('xy{enter}z');
    view.getSearchField().find('.cm-line').should('have.length', 1);
    view.getSearchField().should('have.text', 'xyz');
  });

  it('flattens multi-line pasted text to a single line', () => {
    view.getSearchField().then(($el) => {
      const data = new DataTransfer();
      data.setData('text/plain', 'key:\nfoo');
      $el[0].dispatchEvent(
        new ClipboardEvent('paste', {
          clipboardData: data,
          bubbles: true,
          cancelable: true,
        })
      );
    });
    view.getSearchField().find('.cm-line').should('have.length', 1);
    view.getSearchField().should('have.text', 'key: foo');
  });

  it('clears the search with Escape', () => {
    view.searchFor('key:cart*');
    view.getKeyCell('my.cart').should('not.exist');
    view.getSearchField().type('{esc}');
    view.getSearchField().find('.cm-placeholder').should('exist');
    view.getKeyCell('my.cart').should('be.visible');
  });

  it('clears the search with the clear button', () => {
    view.searchFor('key:cart*');
    view.getKeyCell('my.cart').should('not.exist');
    view.clearSearchWithButton();
    view.getKeyCell('my.cart').should('be.visible');
  });

  it('shows syntax help popover with docs link', () => {
    view
      .getSearchHelp()
      .should('be.visible')
      .and('contain', 'description:cart');
    cy.gcy('translations-search-help-docs-link')
      .should('have.attr', 'href')
      .and('include', 'docs.tolgee.io');
  });

  it('suggests qualifiers after two letters and completes on click', () => {
    view.searchFor('d');
    view.getSearchSuggestions().should('not.exist');

    view.getSearchField().type('e');
    view
      .getSearchSuggestions()
      .should('be.visible')
      .contains('description:')
      .click();
    view.getSearchField().should('have.text', 'description:');
  });

  it('keeps the suggestion visible on an exact qualifier match', () => {
    view.searchFor('de');
    view.getSearchSuggestions().should('be.visible');
    view.getSearchField().type(':');
    view.getSearchSuggestions().should('be.visible');
    view.getSearchField().type('x');
    view.getSearchSuggestions().should('not.exist');
  });

  it('completes qualifier with keyboard', () => {
    view.searchFor('key');
    view.getSearchSuggestions().should('be.visible');
    view.getSearchField().type('{enter}');
    view.getSearchField().should('have.text', 'key:');
    view.getSearchField().type('cart*');
    view.getKeyCell('cart.title').should('be.visible');
    view.getKeyCell('my.cart').should('not.exist');
  });
});
