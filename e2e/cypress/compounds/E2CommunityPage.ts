import { gcy } from '../common/shared';

export class E2CommunityPage {
  hoverLastTopContributor() {
    return gcy('community-top-contributor')
      .last()
      .trigger('mouseover')
      .invoke('attr', 'data-cy-user-id');
  }
}
