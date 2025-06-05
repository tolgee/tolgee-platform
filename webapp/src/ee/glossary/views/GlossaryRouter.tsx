import { Redirect, Route, Switch, useRouteMatch } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { GlossaryContext } from 'tg.ee.module/glossary/hooks/GlossaryContext';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { GlossaryView } from 'tg.ee.module/glossary/views/GlossaryView';

export const GlossaryRouter = () => {
  const organization = useOrganization();
  const match = useRouteMatch();
  const glossaryId = Number(match.params[PARAMS.GLOSSARY_ID]);

  if (isNaN(glossaryId)) {
    return (
      <Redirect
        to={LINKS.ORGANIZATION_GLOSSARIES.build({
          [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
        })}
      />
    );
  }

  return (
    <GlossaryContext organizationId={organization?.id} glossaryId={glossaryId}>
      <Switch>
        <Route exact path={LINKS.ORGANIZATION_GLOSSARY_VIEW.template}>
          <GlossaryView />
        </Route>
      </Switch>
    </GlossaryContext>
  );
};
