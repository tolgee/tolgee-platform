import {
  Box,
  Chip,
  Grid,
  IconButton,
  Tooltip,
  Typography,
  useMediaQuery,
  useTheme,
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
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'grid',
    gridTemplateColumns: `${
      theme.spacing(2) + 50
    }px 150px 100px 5fr 1.5fr 70px`,
    gridTemplateAreas: `
      "image title keyCount stats languages  controls"
    `,
    padding: theme.spacing(3, 2.5),
    cursor: 'pointer',
    overflow: 'hidden',
    '&:hover': {
      backgroundColor: theme.palette.grey['50'],
      '& $translationsIconButton': {
        opacity: 1,
      },
    },
    [theme.breakpoints.down('sm')]: {
      gridTemplateColumns: 'auto 1fr 1fr 70px',
      gridTemplateAreas: `
        "image title keyCount  controls"
        "image title languages controls"
        "image stats stats     stats"
      `,
    },
    [theme.breakpoints.down('xs')]: {
      gridGap: theme.spacing(1, 2),
      gridTemplateColumns: 'auto 1fr 70px',
      gridTemplateAreas: `
        "image     title     controls"
        "image     keyCount  controls"
        "languages languages languages"
        "stats     stats     stats"
      `,
    },
  },
  image: {
    gridArea: 'image',
    overflow: 'hidden',
    marginRight: theme.spacing(2),
    [theme.breakpoints.down('xs')]: {
      marginRight: 0,
    },
  },
  title: {
    gridArea: 'title',
    display: 'flex',
    flexDirection: 'column',
    overflow: 'hidden',
    marginRight: theme.spacing(2),
    [theme.breakpoints.down('xs')]: {
      marginRight: 0,
    },
  },
  keyCount: {
    gridArea: 'keyCount',
    display: 'flex',
    justifyContent: 'flex-end',
    [theme.breakpoints.down('sm')]: {
      justifyContent: 'flex-start',
    },
  },
  stats: {
    gridArea: 'stats',
    display: 'flex',
    paddingTop: theme.spacing(1),
    margin: theme.spacing(0, 6),
    [theme.breakpoints.down('sm')]: {
      margin: 0,
    },
  },
  languages: {
    gridArea: 'languages',
    [theme.breakpoints.down('sm')]: {
      justifyContent: 'flex-start',
    },
  },
  controls: {
    gridArea: 'controls',
  },
  projectName: {
    fontSize: 16,
    fontWeight: 'bold',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    wordBreak: 'break-word',
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
  const theme = useTheme();
  const isCompact = useMediaQuery(theme.breakpoints.down('sm'));

  return (
    <div
      className={classes.container}
      data-cy="dashboard-projects-list-item"
      onClick={() =>
        history.push(
          LINKS.PROJECT_TRANSLATIONS.build({
            [PARAMS.PROJECT_ID]: p.id,
          })
        )
      }
    >
      <div className={classes.image}>
        <AvatarImg
          owner={{
            name: p.name,
            avatar: p.avatar,
            type: 'PROJECT',
            id: p.id,
          }}
          autoAvatarType="INITIALS"
          size={50}
        />
      </div>
      <div className={classes.title}>
        <Typography variant="h3" className={classes.projectName}>
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
      </div>
      <div className={classes.keyCount}>
        <Typography variant="body1">
          <T parameters={{ keysCount: p.stats.keyCount.toString() }}>
            project_list_keys_count
          </T>
        </Typography>
      </div>
      <div className={classes.stats}>
        <TranslationStatesBar stats={p.stats as any} labels={!isCompact} />
      </div>
      <div className={classes.languages} data-cy="project-list-languages">
        <Grid container>
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
      </div>
      <div className={classes.controls}>
        <Box width="100%" display="flex" justifyContent="flex-end">
          <Tooltip title={t('project_list_translations_button')}>
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
      </div>
    </div>
  );
};

export default DashboardProjectListItem;
