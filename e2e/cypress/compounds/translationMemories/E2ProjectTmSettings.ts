import { HOST } from '../../common/constants';
import { gcy } from '../../common/shared';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';

export class E2ProjectTmSettings {
  visit(projectId: number) {
    cy.visit(`${HOST}/projects/${projectId}/manage/edit/advanced`);
  }

  findAndVisit(data: TestDataStandardResponse, projectName: string) {
    const project = data.projects.find((p) => p.name === projectName);
    this.visit(project!.id);
  }

  getSharedSection() {
    return gcy('project-settings-tm-shared');
  }

  getTable() {
    return gcy('project-settings-tm-table');
  }

  getTmRows() {
    return gcy('project-settings-tm-row');
  }

  clickManageAllTms() {
    gcy('project-settings-tm-configure').click();
  }
}
