import {Route, Switch, useRouteMatch} from 'react-router-dom';
import * as React from 'react';
import {LanguageListView} from './languages/LanguageListView';
import {LINKS, PARAMS} from '../../../constants/links';
import {LanguageEditView} from './languages/LanguageEditView';
import {ProjectInviteView} from './invitations/ProjectInviteView';
import {ProjectPermissionsView} from './permissions/ProjectPermissionsVIew';
import {ProjectSettingsView} from './project/ProjectSettingsView';
import {ProjectProvider} from '../../../hooks/ProjectProvider';
import {LanguageCreateView} from './languages/LanguageCreateView';
import {PrivateRoute} from '../../common/PrivateRoute';
import {ImportView} from './imprt_export/ImportView';
import {ExportView} from './imprt_export/ExportView';
import {ProjectPage} from './ProjectPage';
import {Box} from '@material-ui/core';
import {BoxLoading} from '../../common/BoxLoading';
import {TranslationView} from './translations/TranslationView';

export const ProjectRouter = () => {
  let match = useRouteMatch();

  return (
    <Switch>
      <ProjectProvider id={match.params[PARAMS.REPOSITORY_ID]}>
        <ProjectPage fullWidth={true}>
          <Route path={LINKS.REPOSITORY_TRANSLATIONS.template}>
            <React.Suspense
              fallback={
                <Box>
                  <BoxLoading />
                </Box>
              }
            >
              <TranslationView />
            </React.Suspense>
          </Route>
          <Route exact path={LINKS.REPOSITORY_EDIT.template}>
            <ProjectSettingsView />
          </Route>

          <Route exact path={`${LINKS.REPOSITORY_LANGUAGES.template}`}>
            <LanguageListView />
          </Route>

          <Route exact path={`${LINKS.REPOSITORY_LANGUAGES_CREATE.template}`}>
            <LanguageCreateView />
          </Route>

          <Route exact path={`${LINKS.REPOSITORY_LANGUAGE_EDIT.template}`}>
            <LanguageEditView />
          </Route>

          <Route exact path={`${LINKS.REPOSITORY_INVITATION.template}`}>
            <ProjectInviteView />
          </Route>

          <Route exact path={`${LINKS.REPOSITORY_PERMISSIONS.template}`}>
            <ProjectPermissionsView />
          </Route>

          <PrivateRoute exact path={LINKS.REPOSITORY_IMPORT.template}>
            <ImportView />
          </PrivateRoute>

          <Route exact path={`${LINKS.REPOSITORY_EXPORT.template}`}>
            <ExportView />
          </Route>
        </ProjectPage>
      </ProjectProvider>
    </Switch>
  );
};
