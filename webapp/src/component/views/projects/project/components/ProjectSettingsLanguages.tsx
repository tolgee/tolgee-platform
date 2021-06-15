import { SimplePaginatedHateoasList } from '../../../../common/list/SimplePaginatedHateoasList';
import { Box, ListItem, Typography } from '@material-ui/core';
import ListItemText from '@material-ui/core/ListItemText';
import { FlagImage } from '../../../../languages/FlagImage';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from '../../../../../constants/links';
import { SettingsIconButton } from '../../../../common/buttons/SettingsIconButton';
import { T } from '@tolgee/react';
import { CreateSingleLanguage } from '../../../../languages/CreateSingleLanguage';
import * as React from 'react';
import { useEffect } from 'react';
import { container } from 'tsyringe';
import { LanguageActions } from '../../../../../store/languages/LanguageActions';
import { useProject } from '../../../../../hooks/useProject';

const languageActions = container.resolve(LanguageActions);
export const ProjectSettingsLanguages = () => {
  const createLoadable = languageActions.useSelector((s) => s.loadables.create);
  const project = useProject();

  useEffect(() => {
    if (createLoadable.loaded) {
      languageActions.loadableReset.list.dispatch();
      languageActions.loadableReset.globalList.dispatch();
      languageActions.loadableReset.create.dispatch();
    }
  }, [createLoadable.loaded]);

  return (
    <>
      <SimplePaginatedHateoasList
        loadableName="list"
        dispatchParams={[
          {
            path: {
              projectId: project.id,
            },
          },
        ]}
        actions={languageActions}
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
                  [PARAMS.PROJECT_ID]: project.id,
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
      </Box>
      <Box mt={2} minHeight={400}>
        <CreateSingleLanguage
          autoFocus={false}
          onCancel={() => {}}
          onCreated={() => {}}
        />
      </Box>
    </>
  );
};
