import { gcy, gcyAdvanced } from '../../common/shared';

type CommunityPageItem = 'description' | 'guidelines' | 'share' | 'inContext';

export class E2CommunityPageSettings {
  getItemStates(done: boolean) {
    return gcyAdvanced({
      value: 'project-settings-community-page-item-state',
      done: String(done),
    });
  }

  openEditor(item: CommunityPageItem) {
    return gcyAdvanced({
      value: 'project-settings-community-page-edit',
      item,
    }).click();
  }

  getDialogText() {
    return gcy('community-page-dialog-text').find('textarea').first();
  }

  getSetupDescription() {
    return gcy('community-setup-description').find('textarea').first();
  }

  getInContextSwitch() {
    return gcy('community-in-context-switch').find('input');
  }

  getInContextAppUrl() {
    return gcy('community-in-context-app-url').find('input');
  }
}
