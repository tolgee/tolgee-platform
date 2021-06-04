import { default as React, FunctionComponent, useEffect } from 'react';
import { useRouteMatch } from 'react-router-dom';
import { LINKS, PARAMS } from '../../../../constants/links';
import { BaseView } from '../../../layout/BaseView';
import { Box, Button } from '@material-ui/core';
import { container } from 'tsyringe';
import { ExportActions } from '../../../../store/project/ExportActions';
import { useSelector } from 'react-redux';
import { AppState } from '../../../../store';
import { useProject } from '../../../../hooks/useProject';
import { Navigation } from '../../../navigation/Navigation';
import { T, useTranslate } from '@tolgee/react';

const actions = container.resolve(ExportActions);

export const ExportView: FunctionComponent = () => {
  const match = useRouteMatch();
  const project = useProject();
  const projectId = match.params[PARAMS.PROJECT_ID];
  const state = useSelector((state: AppState) => state.export.loadables.export);
  const t = useTranslate();

  useEffect(() => {
    if (state.loaded) {
      const url = URL.createObjectURL(state.data);
      const a = document.createElement('a');
      a.href = url;
      a.download = project.name + '.zip';
      a.click();
      actions.loadableReset.export.dispatch();
    }
  }, [state.loading, state.loaded]);

  useEffect(
    () => () => {
      actions.loadableReset.export.dispatch();
    },
    []
  );

  const onJsonExport = () => {
    actions.loadableActions.export.dispatch(projectId);
  };

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
              t('export_translations_title'),
              LINKS.PROJECT_EXPORT.build({
                [PARAMS.PROJECT_ID]: project.id,
              }),
            ],
          ]}
        />
      }
      xs={12}
      md={10}
      lg={8}
    >
      <Box mt={2}>
        <Button
          component="a"
          variant="outlined"
          color="primary"
          onClick={onJsonExport}
        >
          <T>export_to_json_button</T>
        </Button>
      </Box>
    </BaseView>
  );
};
