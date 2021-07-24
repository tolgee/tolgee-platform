import {
  Box,
  Chip,
  Grid,
  IconButton,
  Tooltip,
  Typography,
} from '@material-ui/core';
import { T, useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';
import { ProjectPermissionType } from 'tg.service/response.types';
import { TranslationStatesBar } from 'tg.views/projects/TranslationStatesBar';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import makeStyles from '@material-ui/core/styles/makeStyles';
import { useConfig } from 'tg.hooks/useConfig';
import { Settings } from '@material-ui/icons';

const useStyles = makeStyles((theme) => ({
  root: {
    padding: `${theme.spacing(3)}px ${theme.spacing(2)}px`,
  },
  projectName: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  projectLink: {
    textDecoration: 'none',
    color: 'black',
  },
  centered: {
    display: 'flex',
    alignItems: 'center',
  },
  keyCount: {
    fontSize: 14,
  },
}));
const DashboardProjectListItem = (
  p: components['schemas']['ProjectWithStatsModel']
) => {
  const classes = useStyles();
  const config = useConfig();
  const t = useTranslate();

  return (
    <Grid
      container
      spacing={2}
      className={classes.root}
      data-cy="dashboard-projects-list-item"
    >
      <Grid item lg={2} md={3} sm={4}>
        <Link
          className={classes.projectLink}
          to={LINKS.PROJECT_TRANSLATIONS.build({ [PARAMS.PROJECT_ID]: p.id })}
        >
          <Typography variant={'h3'} className={classes.projectName}>
            {p.name}
          </Typography>
          {config.authentication && (
            <Box mt={0.5}>
              <Chip
                data-cy="project-list-owner"
                size="small"
                label={p.organizationOwnerName || p.userOwner?.name}
              />
            </Box>
          )}
        </Link>
      </Grid>
      <Grid item lg={1} md={1} sm={1} className={classes.centered}>
        <Typography variant={'body1'} className={classes.keyCount}>
          <T parameters={{ keysCount: p.stats.keyCount.toString() }}>
            project_list_keys_count
          </T>
        </Typography>
      </Grid>
      <Grid item lg md sm>
        <TranslationStatesBar stats={p.stats as any} />
      </Grid>
      <Grid item lg={3} md={2} sm={3} className={classes.centered}>
        <Grid container>
          {p.languages.map((l) => (
            <Grid key={l.id} item>
              <Tooltip title={`${l.name} | ${l.originalName}`}>
                <Box m={0.125}>
                  <CircledLanguageIcon size={20} flag={l.flagEmoji} />
                </Box>
              </Tooltip>
            </Grid>
          ))}
        </Grid>
      </Grid>
      <Grid item className={classes.centered}>
        <Box width="100%" display="flex" justifyContent="flex-end">
          {p.computedPermissions === ProjectPermissionType.MANAGE && (
            <IconButton
              component={Link}
              to={LINKS.PROJECT_EDIT.build({ [PARAMS.PROJECT_ID]: p.id })}
              data-cy="project-settings-button"
              aria-label={t('project_settings_button')}
              size="small"
            >
              <Settings />
            </IconButton>
          )}
        </Box>
      </Grid>
    </Grid>
  );
};

export default DashboardProjectListItem;
