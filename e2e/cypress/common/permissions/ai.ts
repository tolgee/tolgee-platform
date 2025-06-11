import { waitForGlobalLoading } from '../loading';
import { confirmStandard, gcy } from '../shared';
import { getCell } from '../translations';
import { ProjectInfo } from './shared';

export function testAi({ project }: ProjectInfo) {
  const scopes = project.computedPermission.scopes;

  gcy('ai-prompt-item-name').contains('Custom prompt').should('be.visible');

  if (scopes.includes('prompts.edit')) {
    gcy('ai-prompts-add-prompt').should('be.visible');

    // renaming prompt works
    gcy('ai-prompt-item-menu').click();
    gcy('ai-prompts-menu-item-rename').click();
    gcy('ai-prompt-rename-name-field').clear().type('Renamed prompt');
    gcy('ai-prompt-rename-save').click();
    waitForGlobalLoading();
    gcy('ai-prompt-item-name').contains('Renamed prompt').should('be.visible');

    // can open existing prompt
    gcy('ai-prompt-item-name').click();
    gcy('ai-prompt-name').contains('Renamed prompt').should('be.visible');
    gcy('project-menu-item-ai').click();
    waitForGlobalLoading();

    // deleting prompt works
    gcy('ai-prompt-item-menu').click();
    gcy('ai-prompts-menu-item-delete').click();
    confirmStandard();
    gcy('ai-prompts-open-playground-button').should('be.visible');

    // can open playground
    gcy('ai-prompts-open-playground-button').click();
    waitForGlobalLoading();

    // can run preview
    getCell('German text 1').click();
    gcy('ai-prompt-preview-button').click();
    waitForGlobalLoading();
    gcy('ai-playground-preview')
      .contains('response from: server-provider')
      .should('be.visible');
  } else {
    gcy('ai-prompts-add-prompt').should('not.exist');
    gcy('ai-prompt-item-menu').should('not.exist');
  }
}
