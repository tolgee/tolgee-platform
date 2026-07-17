import { T, useTranslate } from '@tolgee/react';
import { styled } from '@mui/material';

import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import SearchField from 'tg.component/common/form/fields/SearchField';
import { useWindowTitle } from 'tg.hooks/useWindowTitle';
import { ProjectsList } from 'tg.views/projects/ProjectsList';
import { usePublicProjectsList } from 'tg.views/projects/usePublicProjectsList';
import { CommunityTranslationBanner } from './CommunityTranslationBanner';
import { PublicTopBar } from './PublicTopBar';
import { PUBLIC_CONTENT_MAX_WIDTH } from './publicProjectsLayout';

const StyledLayout = styled('div')`
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: ${({ theme }) => theme.palette.background.default};
`;

const StyledContent = styled('div')`
  container-type: inline-size;
  width: 100%;
  max-width: ${PUBLIC_CONTENT_MAX_WIDTH}px;
  margin: 0 auto;
  padding: ${({ theme }) => theme.spacing(4, 2)};
`;

const StyledSearch = styled('div')`
  display: flex;
  padding: ${({ theme }) => theme.spacing(0, 0, 3)};
  & > * {
    width: 220px;
  }
`;

export const PublicProjectListView = () => {
  const { t } = useTranslate();
  const { loadable, showSearch, search, onSearch, onPageChange } =
    usePublicProjectsList();

  useWindowTitle(t('public_projects_window_title'));

  return (
    <StyledLayout>
      <PublicTopBar />
      <CommunityTranslationBanner />
      <StyledContent>
        {showSearch && (
          <StyledSearch>
            <SearchField
              initial={search}
              onSearch={onSearch}
              variant="outlined"
              size="small"
            />
          </StyledSearch>
        )}
        <ProjectsList
          variant="public"
          loadable={loadable}
          onPageChange={onPageChange}
          emptyPlaceholder={
            <EmptyListMessage loading={loadable.isFetching}>
              <T
                keyName="public_projects_empty"
              />
            </EmptyListMessage>
          }
        />
      </StyledContent>
    </StyledLayout>
  );
};

export default PublicProjectListView;
