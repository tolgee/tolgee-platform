import { useState, useCallback } from 'react';
import { useTranslate } from '@tolgee/react';

import { OrganizationItem } from './OrganizationItem';
import { components } from 'tg.service/apiSchema.generated';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { useConfig, useIsAdmin } from 'tg.globalContext/helpers';
import { SwitchPopover } from 'tg.component/SwitchPopover/SwitchPopover';

type OrganizationModel = components['schemas']['OrganizationModel'];

type Props = {
  open: boolean;
  onClose: () => void;
  onSelect: (value: OrganizationModel) => void;
  anchorEl: HTMLElement;
  selected: OrganizationModel | undefined;
  onAddNew: () => void;
  ownedOnly?: boolean;
};

export const OrganizationPopover: React.FC<Props> = ({
  open,
  onClose,
  onSelect,
  anchorEl,
  selected,
  onAddNew,
  ownedOnly,
}) => {
  const { t } = useTranslate();
  const [search, setSearch] = useState('');

  const config = useConfig();
  const canCreateOrganizations =
    useIsAdmin() || config.userCanCreateOrganizations;

  const query = {
    filterCurrentUserOwner: Boolean(ownedOnly),
    search: search || undefined,
    size: 20,
    sort: ['name'],
  };

  const organizationsLoadable = useApiInfiniteQuery({
    url: '/v2/organizations',
    method: 'get',
    query,
    options: {
      keepPreviousData: true,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            query: {
              ...query,
              page: lastPage.page!.number! + 1,
            },
          };
        } else {
          return null;
        }
      },
    },
  });

  const items: OrganizationModel[] = organizationsLoadable.data?.pages
    .flatMap((page) => page._embedded?.organizations)
    .filter(Boolean) as OrganizationModel[];

  const totalElements =
    organizationsLoadable.data?.pages[0]?.page?.totalElements ?? 0;

  const handleSearchChange = useCallback((value: string) => {
    setSearch(value);
  }, []);

  if (!selected) {
    return null;
  }

  return (
    <SwitchPopover
      open={open}
      onClose={onClose}
      onSelect={onSelect}
      anchorEl={anchorEl}
      selectedId={selected.id}
      items={items || []}
      isLoading={organizationsLoadable.isFetching}
      hasNextPage={organizationsLoadable.hasNextPage ?? false}
      fetchNextPage={() => organizationsLoadable.fetchNextPage()}
      totalElements={totalElements}
      renderItem={(item) => <OrganizationItem data={item} />}
      searchPlaceholder={t('global_search_organization')}
      headingText={t('organizations_title')}
      dataCyPrefix="organization-switch"
      onSearchChange={handleSearchChange}
      onAddNew={canCreateOrganizations ? onAddNew : undefined}
      addNewTooltip={t('organizations_add_new')}
    />
  );
};
