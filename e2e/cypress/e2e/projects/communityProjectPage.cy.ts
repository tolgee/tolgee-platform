import { HOST } from '../../common/constants';
import { login } from '../../common/apiCalls/common';
import { communityStatsData } from '../../common/apiCalls/testData/testData';
import { gcy } from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';

const OWNER = 'communityStatsOwner';
const PROJECT_NAME = 'Community Stats Demo';

describe('Community project page', () => {
  let projectId: number;

  beforeEach(() => {
    communityStatsData.clean();
    communityStatsData.generateStandard().then((res) => {
      projectId = res.body.projects.find((p) => p.name === PROJECT_NAME).id;
      login(OWNER, 'admin');
    });
  });

  afterEach(() => {
    communityStatsData.clean();
  });

  const visitPage = () => {
    cy.visit(`${HOST}/projects/${projectId}/community`);
    waitForGlobalLoading();
  };

  it('shows top contributors with their period and links to all contributors', () => {
    visitPage();
    gcy('community-top-contributor').should('have.length', 3);
    gcy('community-top-contributors-period')
      .invoke('attr', 'data-cy-period')
      .then((period) => {
        expect(['month', 'year']).to.include(period);
        gcy('community-all-contributors')
          .should('have.attr', 'href')
          .and('include', `/community/contributors?period=${period}`);
      });
  });

  it('collapses and expands a sidebar section', () => {
    visitPage();
    gcy('community-top-contributors').within(() => {
      gcy('community-section-toggle').click();
    });
    gcy('community-top-contributor').should('not.exist');
    gcy('community-top-contributors').within(() => {
      gcy('community-section-toggle').click();
    });
    gcy('community-top-contributor').should('have.length', 3);
  });
});
