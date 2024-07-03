import { FunctionComponent } from 'react';
import {
  Box,
  Button,
  Grid,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { CheckDone01 } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';

import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

export const ImportConflictsDataHeader: FunctionComponent<{
  language: components['schemas']['ImportLanguageModel'];
}> = (props) => {
  const project = useProject();

  const theme = useTheme();
  const rightPanelWidth = useGlobalContext((c) => c.layout.rightPanelWidth);
  const isSmOrLower = useMediaQuery(
    `@media(max-width: ${899 + rightPanelWidth}px)`
  );

  const setOverrideMutation = useApiMutation({
    url: '/v2/projects/{projectId}/import/result/languages/{languageId}/resolve-all/set-override',
    method: 'put',
    invalidatePrefix:
      '/v2/projects/{projectId}/import/result/languages/{languageId}',
  });

  const setKeepMutation = useApiMutation({
    url: '/v2/projects/{projectId}/import/result/languages/{languageId}/resolve-all/set-keep-existing',
    method: 'put',
    invalidatePrefix:
      '/v2/projects/{projectId}/import/result/languages/{languageId}',
  });

  const keepAllExisting = () => {
    setKeepMutation.mutate({
      path: {
        projectId: project.id,
        languageId: props.language!.id,
      },
    });
  };

  const overrideAll = () => {
    setOverrideMutation.mutate({
      path: {
        projectId: project.id,
        languageId: props.language!.id,
      },
    });
  };

  const keepAllButton = (
    <Button
      data-cy="import-resolution-dialog-accept-old-button"
      fullWidth={isSmOrLower}
      startIcon={<CheckDone01 />}
      variant="outlined"
      color="inherit"
      onClick={keepAllExisting}
    >
      <T keyName="import_resolution_accept_old" />
    </Button>
  );
  const overrideAllButton = (
    <Button
      data-cy="import-resolution-dialog-accept-imported-button"
      fullWidth={isSmOrLower}
      startIcon={<CheckDone01 />}
      variant="outlined"
      color="inherit"
      onClick={overrideAll}
    >
      <T keyName="import_resolution_accept_imported" />
    </Button>
  );

  return (
    <Box
      pl={2}
      pt={2}
      pb={2}
      pr={2}
      mb={1}
      style={{
        borderBottom: `1px solid ${theme.palette.emphasis['200']}`,
      }}
    >
      {!isSmOrLower ? (
        <Grid container spacing={2} alignContent="center" alignItems="center">
          <Grid item lg={3} md>
            <Box pl={1}>
              <Typography>
                <b>
                  <T keyName="import_resolve_header_key" />
                </b>
              </Typography>
            </Box>
          </Grid>
          <Grid item lg md sm={12} xs={12}>
            <Box display="flex" alignItems="center">
              <Box pl={1} flexGrow={1}>
                <Typography>
                  <b>
                    <T keyName="import_resolve_header_existing" />
                  </b>
                </Typography>
              </Box>
              {keepAllButton}
            </Box>
          </Grid>
          <Grid item lg md sm={12} xs={12}>
            <Box display="flex" alignItems="center">
              <Box flexGrow={1}>
                <Typography>
                  <b>
                    <T keyName="import_resolve_header_new" />
                  </b>
                </Typography>
              </Box>
              {overrideAllButton}
            </Box>
          </Grid>
        </Grid>
      ) : (
        <Grid container spacing={4}>
          <Grid item lg md sm xs>
            {keepAllButton}
          </Grid>
          <Grid item lg md sm xs>
            {overrideAllButton}
          </Grid>
        </Grid>
      )}
    </Box>
  );
};
