import {
  Box,
  Grid,
  IconButton,
  styled,
  Tooltip,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Link, useHistory } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationStatesBar } from 'tg.views/projects/TranslationStatesBar';
import { TranslationIcon } from 'tg.component/CustomIcons';
import { ProjectListItemMenu } from 'tg.views/projects/ProjectListItemMenu';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { ProjectLanguages } from './ProjectLanguages';

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: calc(${({ theme }) => theme.spacing(2)} + 50px) 150px 100px 5fr 1.5fr 70px;
  grid-template-areas: 'image title keyCount stats languages controls';
  padding: ${({ theme }) => theme.spacing(3, 2.5)};
  cursor: pointer;
  overflow: hidden;
  & .translationIconButton {
    opacity: 0;
    transition: opacity 0.2s ease-in-out;
  }
  &:hover {
    background-color: ${({ theme }) => theme.palette.emphasis['50']};
    & .translationIconButton {
      opacity: 1;
    }
  }
  ${({ theme }) => theme.breakpoints.down('md')} {
    grid-template-columns: auto 1fr 1fr 70px;
    grid-template-areas:
      'image title keyCount  controls'
      'image title languages controls'
      'image stats stats     stats';
  }
  ${({ theme }) => theme.breakpoints.down('sm')} {
    grid-gap: ${({ theme }) => theme.spacing(1, 2)};
    grid-template-columns: auto 1fr 70px;
    grid-template-areas:
      'image     title     controls'
      'image     keyCount  controls'
      'languages languages languages'
      'stats     stats     stats';
  }
`;

const StyledImage = styled('div')`
  grid-area: image;
  overflow: hidden;
  margin-right: ${({ theme }) => theme.spacing(2)};
  ${({ theme }) => theme.breakpoints.down('sm')} {
    margin-right: 0px;
  }
`;

const StyledTitle = styled('div')`
  grid-area: title;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  margin-right: ${({ theme }) => theme.spacing(2)};
  ${({ theme }) => theme.breakpoints.down('sm')} {
    margin-right: 0px;
  }
`;

const StyledKeyCount = styled('div')`
  grid-area: keyCount;
  display: flex;
  justify-content: flex-end;
  ${({ theme }) => theme.breakpoints.down('md')} {
    justify-content: flex-start;
  }
`;

const StyledStats = styled('div')`
  grid-area: stats;
  display: flex;
  padding-top: ${({ theme }) => theme.spacing(1)};
  margin: ${({ theme }) => theme.spacing(0, 6)};
  ${({ theme }) => theme.breakpoints.down('md')} {
    margin: 0px;
  }
`;

const StyledLanguages = styled('div')`
  grid-area: languages;
  ${({ theme }) => theme.breakpoints.down('md')} {
    justify-content: flex-start;
  }
`;

const StyledControls = styled('div')`
  grid-area: controls;
`;

const StyledProjectName = styled(Typography)`
  font-size: 16px;
  font-weight: bold;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-word;
`;

type ProjectWithStatsModel = components['schemas']['ProjectWithStatsModel'];

const DashboardProjectListItem = (p: ProjectWithStatsModel) => {
  const { t } = useTranslate();
  const translationsLink = LINKS.PROJECT_TRANSLATIONS.build({
    [PARAMS.PROJECT_ID]: p.id,
  });
  const history = useHistory();
  const theme = useTheme();
  const isCompact = useMediaQuery(theme.breakpoints.down('md'));

  return (
    <StyledContainer
      data-cy="dashboard-projects-list-item"
      onClick={() =>
        history.push(
          LINKS.PROJECT_DASHBOARD.build({
            [PARAMS.PROJECT_ID]: p.id,
          })
        )
      }
    >
      <StyledImage>
        <AvatarImg
          owner={{
            name: p.name,
            avatar: p.avatar,
            type: 'PROJECT',
            id: p.id,
          }}
          size={50}
        />
      </StyledImage>
      <StyledTitle>
        <StyledProjectName variant="h3">{p.name}</StyledProjectName>
      </StyledTitle>
      <StyledKeyCount>
        <Typography variant="body1">
          <T params={{ keysCount: p.stats.keyCount.toString() }}>
            project_list_keys_count
          </T>
        </Typography>
      </StyledKeyCount>
      <StyledStats>
        <TranslationStatesBar stats={p.stats as any} labels={!isCompact} />
      </StyledStats>
      <StyledLanguages data-cy="project-list-languages">
        <Grid container>
          <ProjectLanguages p={p} />
        </Grid>
      </StyledLanguages>
      <StyledControls>
        <Box width="100%" display="flex" justifyContent="flex-end">
          <Tooltip title={t('project_list_translations_button')}>
            <IconButton
              data-cy="project-list-translations-button"
              onClick={stopBubble()}
              aria-label={t('project_list_translations_button')}
              component={Link}
              to={translationsLink}
              size="small"
              className="translationIconButton"
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
      </StyledControls>
    </StyledContainer>
  );
};

export default DashboardProjectListItem;
