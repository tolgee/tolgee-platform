import { Box, ListItem, Typography } from '@material-ui/core';
import { container } from 'tsyringe';
import { LINKS, PARAMS } from '../../../../constants/links';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import ListItemText from '@material-ui/core/ListItemText';
import { SettingsIconButton } from '../../../common/buttons/SettingsIconButton';
import { Link, useRouteMatch } from 'react-router-dom';
import { LanguageActions } from '../../../../store/languages/LanguageActions';
import { BaseView } from '../../../layout/BaseView';
import { T, useTranslate } from '@tolgee/react';
import { useProject } from '../../../../hooks/useProject';
import { Navigation } from '../../../navigation/Navigation';
import { SimplePaginatedHateoasList } from '../../../common/list/SimplePaginatedHateoasList';
import { LanguageCreate } from '../../../languages/LanguageCreate';
import { useEffect } from 'react';
import { FlagImage } from '../../../languages/FlagImage';

const actions = container.resolve(LanguageActions);
export const LanguageListView = () => {
  const match = useRouteMatch();
  const projectId = match.params[PARAMS.PROJECT_ID];
  const loadable = actions.useSelector((s) => s.loadables.list);
  const project = useProject();
  const t = useTranslate();

  const createLoadable = actions.useSelector((s) => s.loadables.create);

  useEffect(() => {
    if (createLoadable.loaded) {
      actions.loadableReset.list.dispatch();
      actions.loadableReset.globalList.dispatch();
      actions.loadableReset.create.dispatch();
    }
  }, [createLoadable.loaded]);

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
      hideChildrenOnLoading={false}
      lg={5}
      md={7}
    >
      <SimplePaginatedHateoasList
        loadableName="list"
        dispatchParams={[
          {
            path: {
              projectId: projectId,
            },
          },
        ]}
        actions={actions}
        renderItem={(l) => (
          <ListItem key={l.id}>
            <ListItemText>
              <Box display="inline-flex" justifyContent="center">
                <Box mr={1} display="inline-flex" justifyContent="center">
                  <FlagImage width={20} flagEmoji={l.flagEmoji || '🏁'} />
                </Box>
                {l.name} | {l.originalName} ({l.tag})
              </Box>
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
        )}
      />

      <Box mt={4}>
        <Typography variant="h5">
          <T>create_language_title</T>
        </Typography>
        <Box mt={1} minHeight={400}>
          <LanguageCreate onCancel={() => {}} onCreated={() => {}} />
        </Box>
      </Box>
    </BaseView>
  );
};
