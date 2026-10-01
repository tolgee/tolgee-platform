const renderChange = (keyName: string) => {
  const html = (name: string) =>
    `<div data-cy="project-branch-merge-change"><span data-cy="translations-key-name">${name}</span></div>`;
  cy.document().then((doc) => {
    doc.body.innerHTML = html(keyName);
    setTimeout(() => {
      doc.body.innerHTML = html(`${keyName} rerendered`);
    }, 300);
  });
};

describe('Custom data-cy queries', () => {
  it('findDcy retries after the parent is re-rendered', () => {
    renderChange('key');
    cy.gcy('project-branch-merge-change')
      .findDcy('translations-key-name')
      .contains('key rerendered', { timeout: 3000 });
  });

  it('closestDcy retries after the element is re-rendered', () => {
    renderChange('key');
    cy.gcy('translations-key-name')
      .closestDcy('project-branch-merge-change')
      .should('contain', 'key rerendered', { timeout: 3000 });
  });
});
