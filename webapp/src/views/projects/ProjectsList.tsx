import { ReactNode } from 'react';
import { styled } from '@mui/material';
import { UseQueryResult } from 'react-query';

import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { HateoasPaginatedData } from 'tg.service/response.types';
import { components } from 'tg.service/apiSchema.generated';
import DashboardProjectListItem from 'tg.views/projects/DashboardProjectListItem';

type ProjectWithStatsModel = components['schemas']['ProjectWithStatsModel'];

const StyledWrapper = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;

  & .listWrapper > * > * + * {
    border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  }
`;

type Props = {
  loadable: UseQueryResult<HateoasPaginatedData<ProjectWithStatsModel>, any>;
  onPageChange: (page: number) => void;
  emptyPlaceholder: ReactNode;
  variant?: 'default' | 'public';
};

export const ProjectsList = ({
  loadable,
  onPageChange,
  emptyPlaceholder,
  variant = 'default',
}: Props) => {
  return (
    <StyledWrapper>
      <PaginatedHateoasList
        wrapperComponentProps={{ className: 'listWrapper' }}
        onPageChange={onPageChange}
        loadable={loadable}
        renderItem={(r: ProjectWithStatsModel) => (
          <DashboardProjectListItem key={r.id} variant={variant} {...r} />
        )}
        emptyPlaceholder={emptyPlaceholder}
      />
    </StyledWrapper>
  );
};
