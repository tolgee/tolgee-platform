import { ProjectDTO } from '../../../../webapp/src/service/response.types';

import {
  create4Translations,
  selectLangsInLocalstorage,
  translationsBeforeEach,
  visitTranslations,
} from '../../common/translations';
import { assertTooltip, gcy, selectInProjectMenu } from '../../common/shared';
import {
  getCell,
  getStateIndicator,
  setStateToReviewed,
  stateColors,
} from '../../common/state';
import { deleteProject } from '../../common/apiCalls/common';

describe('Translation states', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    translationsBeforeEach()
      .then((p) => (project = p))
      .then(() => {
        create4Translations(project.id);
        visit();
      });
  });

  afterEach(() => {
    deleteProject(project.id);
  });

  it('shows state indicator', () => {
    assertHasState('Cool translated text 1', 'Translated');
  });

  it('changes state to reviewed', () => {
    selectLangsInLocalstorage(project.id, ['en', 'cs']);
    const text = 'Studený přeložený text 2';
    setStateToReviewed(text);
    assertHasState(text, 'Reviewed');
    assertPercentageProjectList({ stateName: 'Reviewed', percentage: 25 });
  });

  it('changes state to need review', () => {
    setStateToReviewed('Cool translated text 2');
    getCell('Cool translated text 2')
      .trigger('mouseover')
      .findDcy('translation-state-button')
      .click();
    assertHasState('Cool translated text 2', 'Translated');
  });

  const assertHasState = (
    translationText: string,
    stateName: keyof typeof stateColors
  ) => {
    getStateIndicator(translationText).trigger('mouseover');
    getStateIndicator(translationText)
      .find('div')
      .should('have.css', 'border-left', `4px solid ${stateColors[stateName]}`);
    assertTooltip(stateName);
    getStateIndicator(translationText).trigger('mouseout');
    visit();
    cy.contains(translationText).should('be.visible');
  };

  const assertPercentageProjectList = (props: {
    stateName: string;
    percentage: number;
  }) => {
    selectInProjectMenu('Projects');
    gcy('project-states-bar-legend').contains(
      `${props.stateName}: ${props.percentage}%`
    );
  };

  const visit = () => {
    visitTranslations(project.id);
  };
});
