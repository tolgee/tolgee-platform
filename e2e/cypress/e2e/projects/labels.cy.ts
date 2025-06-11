import { login } from '../../common/apiCalls/common';
import { labelsTestData } from '../../common/apiCalls/testData/testData';
import { gcy } from '../../common/shared';
import { E2ProjectLabelsSection } from '../../compounds/projectSettings/labels/E2ProjectLabelsSection';

let projectId = null;
let secondProjectId = null;

describe('Projects Settings - Labels', () => {
  const projectLabels = new E2ProjectLabelsSection();
  beforeEach(() => {
    labelsTestData.clean();
    labelsTestData.generate().then((data) => {
      login('test_username');
      projectId = data.body.projects[0].id;
      secondProjectId = data.body.projects[1].id;
    });
  });

  it('list project labels', () => {
    projectLabels.openFromProjectSettings(projectId);
    projectLabels.assertLabelExists('First label', 'This is a description');
  });

  it('should create a new project label', () => {
    projectLabels.visit(projectId);

    const labelModal = projectLabels.openCreateLabelModal();
    labelModal.assertDefaultColorIsFilled();
    labelModal.fillAndSave('test-label', '#FF0055', 'New label description');

    projectLabels.assertLabelsCount(2);
    projectLabels.assertLabelExists('test-label', 'New label description');
  });

  it('should edit an existing label', () => {
    projectLabels.visit(projectId);

    const labelModal = projectLabels.openEditLabelModal('First label');
    labelModal.fillAndSave(
      'Edited label',
      '#00FF00',
      'Edited label description'
    );

    projectLabels.assertLabelExists('Edited label', 'Edited label description');
  });

  it('edit project label with predefined color', () => {
    projectLabels.visit(projectId);

    const labelModal = projectLabels.openEditLabelModal('First label');
    labelModal.fillAndSave('Edited label', { index: 3, hex: '#1188FF' });

    projectLabels.assertLabelExists('Edited label', null, 'rgb(17, 136, 255)');
  });

  it('should delete a label', () => {
    projectLabels.visit(projectId);

    projectLabels.deleteLabel('First label');
    projectLabels.assertLabelsCount(0);
  });

  it('shows paginated list of labels', () => {
    projectLabels.visit(secondProjectId);
    gcy('global-list-pagination').should('be.visible');
    projectLabels.assertLabelsCount(20);
    gcy('global-list-pagination').within(() => {
      cy.get('button').contains('2').click();
    });
    projectLabels.assertLabelsCount(6);
  });
});
