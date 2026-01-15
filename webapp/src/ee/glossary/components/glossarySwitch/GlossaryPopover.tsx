import { useState, useCallback } from 'react';
import { useTranslate } from '@tolgee/react';

import { GlossaryItem } from './GlossaryItem';
import { components } from 'tg.service/apiSchema.generated';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { SwitchPopover } from 'tg.component/SwitchPopover/SwitchPopover';

type SimpleGlossaryModel = components['schemas']['SimpleGlossaryModel'];

type Props = {
  open: boolean;
  onClose: () => void;
  onSelect: (glossary: SimpleGlossaryModel) => void;
  anchorEl: HTMLElement;
  selectedId: number;
  organizationId: number;
};

export const GlossaryPopover: React.FC<Props> = ({
  open,
  onClose,
  onSelect,
  anchorEl,
  selectedId,
  organizationId,
}) => {
  const { t } = useTranslate();
  const [search, setSearch] = useState('');

  const query = {
    search: search || undefined,
    size: 20,
    sort: ['name'] as string[],
  };

  const glossariesLoadable = useApiInfiniteQuery({
    url: '/v2/organizations/{organizationId}/glossaries',
    method: 'get',
    path: { organizationId },
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

  const items: SimpleGlossaryModel[] = glossariesLoadable.data?.pages
    .flatMap((page) => page._embedded?.glossaries)
    .filter(Boolean) as SimpleGlossaryModel[];

  const totalElements =
    glossariesLoadable.data?.pages[0]?.page?.totalElements ?? 0;

  const handleSearchChange = useCallback((value: string) => {
    setSearch(value);
  }, []);

  return (
    <SwitchPopover
      open={open}
      onClose={onClose}
      onSelect={onSelect}
      anchorEl={anchorEl}
      selectedId={selectedId}
      items={items || []}
      isLoading={glossariesLoadable.isFetching}
      hasNextPage={glossariesLoadable.hasNextPage ?? false}
      fetchNextPage={() => glossariesLoadable.fetchNextPage()}
      totalElements={totalElements}
      renderItem={(item) => <GlossaryItem data={item} />}
      searchPlaceholder={t('glossary_switch_search_placeholder')}
      headingText={t('organization_glossaries_title')}
      dataCyPrefix="glossary-switch"
      onSearchChange={handleSearchChange}
    />
  );
};
