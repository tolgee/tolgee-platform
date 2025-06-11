import {
  Box,
  Grid,
  IconButton,
  styled,
  Tooltip,
  Typography,
  useMediaQuery,
} from '@mui/material';
import { Translate01 } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';
import { Link, useHistory } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationStatesBar } from 'tg.views/projects/TranslationStatesBar';
import { ProjectListItemMenu } from 'tg.views/projects/ProjectListItemMenu';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { QuickStartHighlight } from 'tg.component/layout/QuickStartGuide/QuickStartHighlight';
import { CircledLanguageIconList } from 'tg.component/languages/CircledLanguageIconList';

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: calc(${({ theme }) => theme.spacing(2)} + 50px) 150px 100px 5fr 1.5fr 70px;
  grid-template-areas: 'image title keyCount stats languages controls';
  padding: ${({ theme }) => theme.spacing(3, 2.5)};
  cursor: pointer;
  background-color: ${({ theme }) => theme.palette.background.default};
  & .translationIconButton {
    opacity: 0;
    transition: opacity 0.2s ease-in-out;
  }
  &:hover {
    background-color: ${({ theme }) => theme.palette.cell.hover};
    transition: background-color 0.1s ease-in;
    & .translationIconButton {
      opacity: 1;
    }
  }
  @container (max-width: 850px) {
    grid-template-columns: auto 1fr 1fr 70px;
    grid-template-areas:
      'image title keyCount  controls'
      'image title languages controls'
      'image stats stats     stats';
  }
  @container (max-width: 599px) {
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
  @container (max-width: 599px) {
    margin-right: 0px;
  }
`;

const StyledTitle = styled('div')`
  grid-area: title;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  margin-right: ${({ theme }) => theme.spacing(2)};
  @container (max-width: 599px) {
    margin-right: 0px;
  }
`;

const StyledKeyCount = styled('div')`
  grid-area: keyCount;
  display: flex;
  justify-content: flex-end;
  @container (max-width: 850px) {
    justify-content: flex-start;
  }
`;

const StyledStats = styled('div')`
  grid-area: stats;
  display: flex;
  padding-top: ${({ theme }) => theme.spacing(1)};
  margin: ${({ theme }) => theme.spacing(0, 6)};
  @container (max-width: 850px) {
    margin: 0px;
  }
`;

const StyledLanguages = styled('div')`
  grid-area: languages;
  @container (max-width: 850px) {
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
  const rightPanelWidth = useGlobalContext((c) => c.layout.rightPanelWidth);
  const isCompact = useMediaQuery(
    `@media(max-width: ${rightPanelWidth + 800}px)`
  );

  return (
    <QuickStartHighlight
      itemKey="demo_project"
      disabled={p.name !== 'Demo project'}
      borderRadius="4px"
      offset={1}
    >
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
            <T
              keyName="project_list_keys_count"
              params={{ keysCount: p.stats.keyCount.toString() }}
            />
          </Typography>
        </StyledKeyCount>
        <StyledStats>
          <TranslationStatesBar stats={p.stats as any} labels={!isCompact} />
        </StyledStats>
        <StyledLanguages data-cy="project-list-languages">
          <Grid container>
            <CircledLanguageIconList languages={p.languages} />
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
                <Translate01 />
              </IconButton>
            </Tooltip>
            <ProjectListItemMenu
              projectId={p.id}
              computedPermission={p.computedPermission}
              projectName={p.name}
            />
          </Box>
        </StyledControls>
      </StyledContainer>
    </QuickStartHighlight>
  );
};

export default DashboardProjectListItem;
