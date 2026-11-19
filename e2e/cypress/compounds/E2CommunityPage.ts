import { gcyAdvanced } from '../common/shared';

export class E2CommunityPage {
  hoverTopContributor(userId: number) {
    gcyAdvanced({
      value: 'community-top-contributor',
      'user-id': String(userId),
    }).trigger('mouseover');
    return this;
  }
}
