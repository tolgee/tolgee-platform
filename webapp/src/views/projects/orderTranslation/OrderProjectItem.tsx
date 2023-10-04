import {
  Checkbox,
  Grid,
  styled,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { T } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationStatesBar } from 'tg.views/projects/TranslationStatesBar';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { ProjectLanguages } from '../ProjectLanguages';

const StyledContent = styled('div')`
  display: grid;
  grid-template-columns: auto 150px 100px 5fr 1.5fr;
  grid-template-areas: 'image title keyCount stats languages';
  padding: ${({ theme }) => theme.spacing(3, 2.5)};
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
  @container (max-width: 850px) {
    grid-template-columns: auto 1fr 1fr 70px;
    grid-template-areas:
      'image title keyCount '
      'image title languages'
      'image stats stats    ';
  }
  @container (max-width: 599px) {
    grid-gap: ${({ theme }) => theme.spacing(1, 2)};
    grid-template-columns: auto 1fr 70px;
    grid-template-areas:
      'image     title    '
      'image     keyCount '
      'languages languages'
      'stats     stats    ';
  }
`;

const StyledCheckboxWrapper = styled('div')`
  grid-area: checkbox;
  width: 60px;
`;

const StyledImage = styled('div')`
  grid-area: image;
  overflow: hidden;
  margin-right: ${({ theme }) => theme.spacing(2)};
  display: flex;
  align-items: center;
  align-self: start;
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

const StyledProjectName = styled(Typography)`
  font-size: 16px;
  font-weight: bold;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-word;
`;

type ProjectWithStatsModel = components['schemas']['ProjectWithStatsModel'];

type Props = {
  project: ProjectWithStatsModel;
  selected: boolean;
  onSelectToggle: () => void;
};

export const OrderProjectItem = ({
  project,
  selected,
  onSelectToggle,
}: Props) => {
  const theme = useTheme();
  const isCompact = useMediaQuery(theme.breakpoints.down('md'));

  return (
    <StyledContent data-cy="dashboard-projects-list-item">
      <StyledImage>
        <StyledCheckboxWrapper>
          <Checkbox checked={selected} onChange={() => onSelectToggle()} />
        </StyledCheckboxWrapper>
        <AvatarImg
          owner={{
            name: project.name,
            avatar: project.avatar,
            type: 'PROJECT',
            id: project.id,
          }}
          size={50}
        />
      </StyledImage>
      <StyledTitle>
        <StyledProjectName variant="h3">{project.name}</StyledProjectName>
      </StyledTitle>
      <StyledKeyCount>
        <Typography variant="body1">
          <T
            keyName="project_list_keys_count"
            params={{ keysCount: project.stats.keyCount.toString() }}
          />
        </Typography>
      </StyledKeyCount>
      <StyledStats>
        <TranslationStatesBar
          stats={project.stats as any}
          labels={!isCompact}
        />
      </StyledStats>
      <StyledLanguages data-cy="project-list-languages">
        <Grid container>
          <ProjectLanguages p={project} />
        </Grid>
      </StyledLanguages>
    </StyledContent>
  );
};
