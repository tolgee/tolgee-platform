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

import { getProjectTranslationsUrl, LINKS, PARAMS } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationStatesBar } from 'tg.views/projects/TranslationStatesBar';
import { ProjectListItemMenu } from 'tg.views/projects/ProjectListItemMenu';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { useEnabledFeatures, useQaCheckTypes } from 'tg.globalContext/helpers';
import { QuickStartHighlight } from 'tg.component/layout/QuickStartGuide/QuickStartHighlight';
import { CircledLanguageIconList } from 'tg.component/languages/CircledLanguageIconList';
import { TransparentChip } from 'tg.component/common/chips/TransparentChip';
import { QaBadge } from 'tg.ee';

const StyledContainer = styled('div', {
  shouldForwardProp: (prop) => prop !== 'isPublicVariant',
})<{ isPublicVariant?: boolean }>`
  display: grid;
  grid-template-columns: ${({ theme, isPublicVariant }) =>
    isPublicVariant
      ? `calc(${theme.spacing(2)} + 50px) 180px 100px 5fr 1.5fr`
      : `calc(${theme.spacing(2)} + 50px) 150px 100px 5fr 1.5fr 70px`};
  grid-template-areas: ${({ isPublicVariant }) =>
    isPublicVariant
      ? "'image title keyCount stats languages'"
      : "'image title keyCount stats languages controls'"};
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
    grid-template-columns: ${({ isPublicVariant }) =>
      isPublicVariant ? 'auto 1fr 1fr' : 'auto 1fr 1fr 70px'};
    grid-template-areas: ${({ isPublicVariant }) =>
      isPublicVariant
        ? "'image title keyCount' 'image title languages' 'image stats stats'"
        : "'image title keyCount  controls' 'image title languages controls' 'image stats stats     stats'"};
  }
  @container (max-width: 599px) {
    grid-gap: ${({ theme }) => theme.spacing(1, 2)};
    grid-template-columns: ${({ isPublicVariant }) =>
      isPublicVariant ? 'auto 1fr' : 'auto 1fr 70px'};
    grid-template-areas: ${({ isPublicVariant }) =>
      isPublicVariant
        ? "'image title' 'image keyCount' 'languages languages' 'stats stats'"
        : "'image     title     controls' 'image     keyCount  controls' 'languages languages languages' 'stats     stats     stats'"};
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

const StyledPublicLine = styled('div')`
  display: flex;
  align-items: center;
  gap: 4px;
  overflow: hidden;
  margin-top: 2px;
`;

const StyledPublicChip = styled(TransparentChip)`
  flex-shrink: 0;
  & .MuiChip-label {
    font-size: 13px;
  }
`;

const StyledOrganizationName = styled(Typography)`
  color: ${({ theme }) => theme.palette.text.secondary};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

type ProjectWithStatsModel = components['schemas']['ProjectWithStatsModel'];

type Props = ProjectWithStatsModel & {
  variant?: 'default' | 'public';
};

const DashboardProjectListItem = ({ variant = 'default', ...p }: Props) => {
  const isPublicVariant = variant === 'public';
  const { t } = useTranslate();
  const history = useHistory();
  const allowPrivate = useGlobalContext((c) => c.auth.allowPrivate);
  const rightPanelWidth = useGlobalContext((c) => c.layout.rightPanelWidth);
  const isCompact = useMediaQuery(
    `@media(max-width: ${rightPanelWidth + 800}px)`
  );
  const { isEnabled } = useEnabledFeatures();
  const allQaCheckTypes = useQaCheckTypes();
  const hasQaIssues = p.stats.qaIssueCount > 0;
  const hasStaleQaChecks = p.stats.qaChecksStaleCount > 0;
  const showQaBadge =
    isEnabled('QA_CHECKS') && (hasQaIssues || hasStaleQaChecks);

  const content = (
    <StyledContainer
      data-cy="dashboard-projects-list-item"
      isPublicVariant={isPublicVariant}
      onClick={() =>
        history.push(
          isPublicVariant && !allowPrivate
            ? LINKS.LOGIN.build()
            : LINKS.PROJECT_DASHBOARD.build({
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
        {p.public && (
          <StyledPublicLine>
            <StyledPublicChip
              size="small"
              label={t('project_list_public_badge')}
              data-cy="project-list-public-badge"
            />
            {p.organizationOwner?.name && (
              <StyledOrganizationName
                variant="body2"
                data-cy="project-list-org-name"
              >
                {p.organizationOwner.name}
              </StyledOrganizationName>
            )}
          </StyledPublicLine>
        )}
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
      {!isPublicVariant && (
        <StyledControls>
          <Box width="100%" display="flex" justifyContent="flex-end">
            {showQaBadge ? (
              <Tooltip title={t('project_list_qa_issues_button')}>
                <IconButton
                  data-cy="project-list-qa-badge-button"
                  onClick={stopBubble()}
                  aria-label={t('project_list_qa_issues_button')}
                  component={Link}
                  to={getProjectTranslationsUrl(p.id, {
                    filters: { filterQaCheckTypes: allQaCheckTypes },
                  })}
                  size="small"
                >
                  <QaBadge
                    count={p.stats.qaIssueCount}
                    stale={hasStaleQaChecks && !p.stats.qaIssueCount}
                    darkWhenNoIssues
                  />
                </IconButton>
              </Tooltip>
            ) : (
              <Tooltip title={t('project_list_translations_button')}>
                <IconButton
                  data-cy="project-list-translations-button"
                  onClick={stopBubble()}
                  aria-label={t('project_list_translations_button')}
                  component={Link}
                  to={getProjectTranslationsUrl(p.id)}
                  size="small"
                  className="translationIconButton"
                >
                  <Translate01 />
                </IconButton>
              </Tooltip>
            )}
            <ProjectListItemMenu
              projectId={p.id}
              computedPermission={p.computedPermission}
              projectName={p.name}
            />
          </Box>
        </StyledControls>
      )}
    </StyledContainer>
  );

  if (isPublicVariant) {
    return content;
  }

  return (
    <QuickStartHighlight
      itemKey="demo_project"
      disabled={p.name !== 'Demo project'}
      borderRadius="4px"
      offset={1}
    >
      {content}
    </QuickStartHighlight>
  );
};

export default DashboardProjectListItem;
