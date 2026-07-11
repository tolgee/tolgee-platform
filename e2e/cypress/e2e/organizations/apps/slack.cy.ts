import {
  login,
  setBypassSeatCountCheck,
} from '../../../common/apiCalls/common';
import { organizationTestData } from '../../../common/apiCalls/testData/testData';
import { gcy } from '../../../common/shared';
import { HOST } from '../../../common/constants';

describe('Slack', () => {
  let organizationData: Record<string, { slug: string }>;

  beforeEach(() => {
    setBypassSeatCountCheck(true);
    login();
    organizationTestData.clean();
    organizationTestData.generate().then((res) => {
      organizationData = res.body as any;
      visitProfile('Tolgee');
    });
  });

  afterEach(() => {
    setBypassSeatCountCheck(false);
  });

  it('slack app settings exist', () => {
    gcy('organization-side-menu').contains('Apps').click();
    cy.contains('Slack').should('be.visible');
  });

  after(() => {
    organizationTestData.clean();
  });

  const visitProfile = (name: string) => {
    visitPath(name, `/profile`);
  };

  function visitSlug(slug: string, path: string) {
    cy.visit(`${HOST}/organizations/${slug}${path}`);
  }

  const visitPath = (name: string, path: string) => {
    const slug = organizationData[name].slug;
    visitSlug(slug, path);
  };
});
