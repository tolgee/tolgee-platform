import { useEffect } from 'react';
import { ListItem } from '@material-ui/core';
import Box from '@material-ui/core/Box';
import { container } from 'tsyringe';
import { LINKS, PARAMS } from '../../../../constants/links';
import { FabAddButtonLink } from '../../../common/buttons/FabAddButtonLink';
import List from '@material-ui/core/List';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import ListItemText from '@material-ui/core/ListItemText';
import { SettingsIconButton } from '../../../common/buttons/SettingsIconButton';
import { Link, useRouteMatch } from 'react-router-dom';
import { LanguageActions } from '../../../../store/languages/LanguageActions';
import { BaseView } from '../../../layout/BaseView';
import { useTranslate } from '@tolgee/react';
import { useProject } from '../../../../hooks/useProject';
import { Navigation } from '../../../navigation/Navigation';

const actions = container.resolve(LanguageActions);

export const LanguageListView = () => {
  const match = useRouteMatch();
  const projectId = match.params[PARAMS.PROJECT_ID];

  const loadable = actions.useSelector((s) => s.loadables.list);

  const project = useProject();

  const t = useTranslate();

  useEffect(() => {
    actions.loadableActions.list.dispatch(projectId);
  }, []);

  return (
    <BaseView
      navigation={
        <Navigation
          path={[
            [
              project.name,
              LINKS.PROJECT_TRANSLATIONS.build({
                [PARAMS.PROJECT_ID]: project.id,
              }),
            ],
            [
              t('languages_title'),
              LINKS.PROJECT_TRANSLATIONS.build({
                [PARAMS.PROJECT_ID]: project.id,
              }),
            ],
          ]}
        />
      }
      loading={loadable.loading || !loadable.touched}
      lg={5}
      md={7}
    >
      {() => (
        <Box ml={-2}>
          <List>
            {loadable.data!.map((l) => (
              <ListItem key={l.id}>
                <ListItemText>
                  {l.name} [{l.abbreviation}]
                </ListItemText>
                <ListItemSecondaryAction>
                  <Link
                    to={LINKS.PROJECT_LANGUAGE_EDIT.build({
                      [PARAMS.PROJECT_ID]: projectId,
                      [PARAMS.LANGUAGE_ID]: l.id,
                    })}
                  >
                    <SettingsIconButton />
                  </Link>
                </ListItemSecondaryAction>
              </ListItem>
            ))}
          </List>
          <Box
            display="flex"
            flexDirection="column"
            alignItems="flex-end"
            pr={2}
            mt={5}
          >
            <FabAddButtonLink
              to={LINKS.PROJECT_LANGUAGES_CREATE.build({
                [PARAMS.PROJECT_ID]: projectId,
              })}
            />
          </Box>
        </Box>
      )}
    </BaseView>
  );
};
