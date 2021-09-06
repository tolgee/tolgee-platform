import {
  Box,
  Chip,
  Grid,
  IconButton,
  Tooltip,
  Typography,
} from '@material-ui/core';
import { T, useTranslate } from '@tolgee/react';
import { useHistory } from 'react-router';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationStatesBar } from 'tg.views/projects/TranslationStatesBar';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import makeStyles from '@material-ui/core/styles/makeStyles';
import { useConfig } from 'tg.hooks/useConfig';
import { TranslationIcon } from 'tg.component/CustomIcons';
import { ProjectListItemMenu } from 'tg.views/projects/ProjectListItemMenu';
import { stopBubble } from 'tg.fixtures/eventHandler';

const useStyles = makeStyles((theme) => ({
  root: {
    padding: `${theme.spacing(3)}px ${theme.spacing(2)}px`,
    cursor: 'pointer',
    overflow: 'hidden',
    '&:hover': {
      backgroundColor: theme.palette.grey['50'],
      '& $translationsIconButton': {
        opacity: 1,
      },
    },
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
    fontSize: 16,
  },
  flagIcon: {
    cursor: 'default',
  },
  translationsIconButton: {
    opacity: 0,
    transition: 'opacity 0.2s ease-in-out',
  },
}));
const DashboardProjectListItem = (
  p: components['schemas']['ProjectWithStatsModel']
) => {
  const classes = useStyles();
  const config = useConfig();
  const t = useTranslate();
  const translationsLink = LINKS.PROJECT_TRANSLATIONS.build({
    [PARAMS.PROJECT_ID]: p.id,
  });
  const history = useHistory();

  return (
    <Box
      className={classes.root}
      onClick={() =>
        history.push(
          LINKS.PROJECT_TRANSLATIONS.build({
            [PARAMS.PROJECT_ID]: p.id,
          })
        )
      }
      data-cy="dashboard-projects-list-item"
    >
      <Grid container spacing={3}>
        <Grid item lg={2} md={2} sm={4} xs={9}>
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
        </Grid>
        <Grid item lg={1} md={1} sm={1} xs={3} className={classes.centered}>
          <Typography className={classes.keyCount}>
            <T parameters={{ keysCount: p.stats.keyCount.toString() }}>
              project_list_keys_count
            </T>
          </Typography>
        </Grid>
        <Grid item lg md sm>
          <TranslationStatesBar stats={p.stats as any} />
        </Grid>
        <Grid item lg={2} md={2} sm={9} xs={9}>
          <Grid container data-cy="project-list-languages">
            {p.languages.map((l) => (
              <Grid key={l.id} item onClick={stopBubble()}>
                <Tooltip title={`${l.name} | ${l.originalName}`}>
                  <Box m={0.125} data-cy="project-list-languages-item">
                    <CircledLanguageIcon
                      className={classes.flagIcon}
                      size={20}
                      flag={l.flagEmoji}
                    />
                  </Box>
                </Tooltip>
              </Grid>
            ))}
          </Grid>
        </Grid>
        <Grid item md={1} sm xs>
          <Box width="100%" display="flex" justifyContent="flex-end">
            <Tooltip
              title={t('project_list_translations_button', undefined, true)}
            >
              <IconButton
                onClick={stopBubble()}
                aria-label={t('project_list_translations_button')}
                component={Link}
                to={translationsLink}
                size="small"
                className={classes.translationsIconButton}
              >
                <TranslationIcon />
              </IconButton>
            </Tooltip>
            <ProjectListItemMenu
              projectId={p.id}
              computedPermissions={p.computedPermissions}
              projectName={p.name}
            />
          </Box>
        </Grid>
      </Grid>
    </Box>
  );
};

export default DashboardProjectListItem;
