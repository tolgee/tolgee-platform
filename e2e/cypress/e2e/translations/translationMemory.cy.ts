import {
  tmSuggestionsTestData,
  getProjectByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { login } from '../../common/apiCalls/common';
import {
  getTranslationCell,
  selectLangsInLocalstorage,
  visitTranslations,
} from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { E2TranslationMemoryToolsPanel } from '../../compounds/translationMemories/E2TranslationMemoryToolsPanel';

describe('Translation memory suggestion panel', () => {
  let data: TestDataStandardResponse;
  let projectId: number;
  const tmPanel = new E2TranslationMemoryToolsPanel();

  beforeEach(() => {
    tmSuggestionsTestData.clean();
    tmSuggestionsTestData.generateStandard().then((res) => {
      data = res.body;
      projectId = getProjectByNameFromTestData(data, 'Suggestions Project')!.id;
      login('tm_suggestions_user');
      selectLangsInLocalstorage(projectId, ['en', 'cs']);
      visitTranslations(projectId);
    });
  });

  afterEach(() => {
    tmSuggestionsTestData.clean();
  });

  it('shows suggestions when editing a translation cell', () => {
    waitForGlobalLoading();
    openCsEditor('multi-source');

    tmPanel.getItemContaining('Ze sdílené TM').should('be.visible');
  });

  it('applies a suggestion target to the editor on click', () => {
    waitForGlobalLoading();
    openCsEditor('multi-source');

    tmPanel.getItemContaining('Ze sdílené TM').first().click();
    cy.gcy('global-editor').contains('Ze sdílené TM').should('be.visible');
  });

  describe('Score badge', () => {
    it('exact match renders 100% with the high tier', () => {
      waitForGlobalLoading();
      openCsEditor('tier-source');

      tmPanel.getScoreOf('Vysoký score').should('contain', '100%');
      tmPanel
        .getScoreOf('Vysoký score')
        .should('have.attr', 'data-tier', '100');
    });
  });

  describe('Penalty', () => {
    it('penalized suggestion shows a reduced score and a penalized tier', () => {
      waitForGlobalLoading();
      openCsEditor('penalty-source');

      // Default penalty 30 against a 100% raw match → displayed 70%.
      tmPanel.getScoreOf('Penalizovaný překlad').should('contain', '70%');
      tmPanel
        .getScoreOf('Penalizovaný překlad')
        .should('have.attr', 'data-tier', 'penalized');
    });

    it('penalty tooltip shows the raw% / penalty breakdown on hover', () => {
      waitForGlobalLoading();
      openCsEditor('penalty-source');

      tmPanel.hoverScoreOf('Penalizovaný překlad');
      tmPanel.getTooltip().should('contain', '100%').and('contain', '30');
    });
  });

  describe('Meta line', () => {
    it('shows the source TM name', () => {
      waitForGlobalLoading();
      openCsEditor('tier-source');

      tmPanel.getMetaTmNameOf('Vysoký score').should('contain', 'Shared TM');
    });

    it('shows the matched key name for a cross-key suggestion', () => {
      waitForGlobalLoading();
      openCsEditor('keyref-source');

      // Project TM (virtual) carries existing-helper-keyref's translation as a 100%
      // cross-key match — the keyName meta segment should expose the helper key's name.
      tmPanel
        .getMetaKeyNameOf('Křížový překlad z projektu')
        .should('contain', 'existing-helper-keyref');
    });

    it('shows a relative time label ("3d ago") for stored entries', () => {
      waitForGlobalLoading();
      openCsEditor('time-source');

      // Entry was post-dated 3 days by the e2e controller (Spring's @LastModifiedDate
      // overwrites the builder's value on persist, so the controller backdates with a
      // native UPDATE).
      tmPanel.getMetaUpdatedAtOf('Čas z TM').should('have.text', '3d ago');
    });
  });

  describe('Permissions', () => {
    it('does not surface suggestions from a TM with readAccess=false', () => {
      waitForGlobalLoading();
      openCsEditor('noread-source');

      // The "No-read shared TM" is the only TM with a matching entry, and its
      // readAccess is disabled for this project — the panel should be empty.
      tmPanel.getItems().should('not.exist');
      cy.contains('Should not appear').should('not.exist');
      tmPanel.getEmptyState().should('be.visible');
    });
  });

  describe('Multiple sources', () => {
    it('renders both project TM and shared TM matches', () => {
      waitForGlobalLoading();
      openCsEditor('multi-source');

      tmPanel.getItemContaining('Z projektu').should('be.visible');
      tmPanel.getItemContaining('Ze sdílené TM').should('be.visible');
    });

    it('project TM (priority 0) ranks above the shared TM', () => {
      waitForGlobalLoading();
      openCsEditor('multi-source');

      // First-rendered item is the highest-priority hit.
      tmPanel.getItems().first().should('contain', 'Z projektu');
    });
  });

  describe('Empty state', () => {
    it('renders "Nothing found" when no TM entry is similar', () => {
      waitForGlobalLoading();
      openCsEditor('empty-source');

      tmPanel.getEmptyState().should('be.visible');
      tmPanel.getItems().should('not.exist');
    });
  });

  const openCsEditor = (keyName: string) => {
    getTranslationCell(keyName, 'cs').click();
    cy.gcy('global-editor').should('be.visible');
  };
});
