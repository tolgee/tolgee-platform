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

  it('shows the top contributors with their period', () => {
    visitPage();
    gcy('community-top-contributor').should('have.length', 3);
    gcy('community-top-contributors-period')
      .invoke('attr', 'data-cy-period')
      .then((period) => {
        expect(['month', 'year']).to.include(period);
      });
  });

  const stubRecentTopTotal = (total: number) => {
    const counts = { year: 0 };
    cy.intercept(
      { method: 'GET', url: /\/community\/contributors\?/ },
      (req) => {
        const params = new URL(req.url).searchParams;
        if (params.get('size') !== '3') return;
        const daysAgo =
          (Date.now() - Date.parse(params.get('from')!)) / 86_400_000;
        if (daysAgo < 60) {
          req.continue((res) => {
            res.body.page.totalElements = total;
          });
          return;
        }
        counts.year++;
      }
    );
    return counts;
  };

  it('shows the last 30 days when at least 3 people contributed in them', () => {
    const counts = stubRecentTopTotal(3);
    visitPage();
    gcy('community-top-contributors-period').should(
      'have.attr',
      'data-cy-period',
      'month'
    );
    cy.wrap(counts).its('year').should('eq', 0);
  });

  it('falls back to the last 12 months when fewer than 3 contributed in the last 30 days', () => {
    const counts = stubRecentTopTotal(2);
    visitPage();
    gcy('community-top-contributors-period').should(
      'have.attr',
      'data-cy-period',
      'year'
    );
    cy.wrap(counts).its('year').should('eq', 1);
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
