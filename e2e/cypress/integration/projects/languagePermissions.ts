import { gcy } from '../../common/shared';
import { enterProject, visitList } from '../../common/projects';
import { waitForGlobalLoading } from '../../common/loading';
import {
  assertAvailableCommands,
  move,
  selectFirst,
} from '../../common/shortcuts';
import { languagePermissionsData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { getCell } from '../../common/state';

describe('Project Permissions', () => {
  beforeEach(() => {
    languagePermissionsData.clean();
    languagePermissionsData.generate();
  });

  afterEach(() => {
    waitForGlobalLoading();
  });

  it('English can be edited', () => {
    login('en_only_user');
    visitList();
    enterProject('Project');
    getCell('english_translation')
      .trigger('mouseover')
      .findDcy('translations-cell-edit-button')
      .should('exist');
    getCell('english_translation').click();
    gcy('global-editor').should('be.visible');
  });

  it('German cannot be edited', () => {
    login('en_only_user');
    visitList();
    enterProject('Project');
    getCell('german_translation')
      .trigger('mouseover')
      .findDcy('translations-cell-edit-button')
      .should('not.exist');
    getCell('german_translation').click();
    gcy('global-editor').should('not.exist');
  });

  it('State cannot be changed for German', () => {
    login('en_only_user');
    visitList();
    enterProject('Project');
    getCell('german_translation')
      .trigger('mouseover')
      .findDcy('translation-state-button')
      .should('not.exist');

    gcy('global-editor').should('not.exist');
  });

  it('Shortcuts are restricted for german', () => {
    login('en_only_user');
    visitList();
    enterProject('Project');
    selectFirst();
    assertAvailableCommands(['Move', 'Edit', 'Reviewed']);
    move('downarrow');
    assertAvailableCommands(['Move']);
  });
});
