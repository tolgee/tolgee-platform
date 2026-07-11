import { glossaryTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { E2GlossaryView } from '../../compounds/glossaries/E2GlossaryView';
import { E2GlossaryTermCreateEditDialog } from '../../compounds/glossaries/E2GlossaryTermCreateEditDialog';

describe('Glossary term editing', () => {
  let view: E2GlossaryView;
  let dialog: E2GlossaryTermCreateEditDialog;

  beforeEach(() => {
    glossaryTestData.clean();
    glossaryTestData.generateStandard().then((res) => {
      login('Owner');
      view = new E2GlossaryView();
      view.findAndVisit(res.body, 'Owner', 'Test Glossary');
      dialog = view.openEditTermDialog('Term');
    });
  });

  afterEach(() => {
    glossaryTestData.clean();
  });

  it('Edits a glossary term name', () => {
    dialog.setDefaultTranslation('Edited Test Term');
    dialog.submit();

    gcy('glossary-term-list-item').should('contain', 'Edited Test Term');
  });

  it('Edits a glossary term description', () => {
    dialog.setDescription('This is an edited description for the test term');
    dialog.submit();

    gcy('glossary-term-list-item').should(
      'contain',
      'This is an edited description for the test term'
    );
  });

  it('Edits glossary term flags', () => {
    dialog.toggleFlagCaseSensitive();
    dialog.toggleFlagAbbreviation();
    dialog.toggleFlagForbidden();
    dialog.submit();

    const dialogAfterEdit = view.openEditTermDialog('Term');
    gcy('create-glossary-term-flag-case-sensitive')
      .find('input')
      .should('be.checked');
    gcy('create-glossary-term-flag-abbreviation')
      .find('input')
      .should('be.checked');
    gcy('create-glossary-term-flag-forbidden')
      .find('input')
      .should('be.checked');
    dialogAfterEdit.cancel();
  });
});
