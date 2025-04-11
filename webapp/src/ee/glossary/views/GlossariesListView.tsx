import { LINKS, PARAMS } from 'tg.constants/links';
import React, { useState } from 'react';
import { BaseOrganizationSettingsView } from 'tg.views/organizations/components/BaseOrganizationSettingsView';
import { useTranslate } from '@tolgee/react';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { GlossaryCreateEditDialog } from 'tg.ee.module/glossary/views/GlossaryCreateEditDialog';
import { GlossaryListItem } from 'tg.ee.module/glossary/components/GlossaryListItem';
import { styled } from '@mui/material';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { GlossariesEmptyListMessage } from 'tg.ee.module/glossary/components/GlossariesEmptyListMessage';

const StyledWrapper = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;

  & .listWrapper > * > * + * {
    border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  }
`;

export const GlossariesListView = () => {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [createDialogOpen, setCreateDialogOpen] = useState(false);

  const organization = useOrganization();

  const { t } = useTranslate();

  const glossaries = useApiQuery({
    url: '/v2/organizations/{organizationId}/glossaries',
    method: 'get',
    path: { organizationId: organization!.id },
    query: {
      page,
      size: 20,
      search,
      sort: ['id,desc'],
    },
    options: {
      keepPreviousData: true,
    },
  });

  const items = glossaries?.data?._embedded?.glossaries;
  const showSearch = search || (glossaries.data?.page?.totalElements ?? 0) > 5;

  const onCreate = () => {
    setCreateDialogOpen(true);
  };

  return (
    <StyledWrapper>
      <BaseOrganizationSettingsView
        windowTitle={t('organization_glossaries_title')}
        onSearch={showSearch ? setSearch : undefined}
        searchPlaceholder={t('glossaries_search_placeholder')}
        link={LINKS.ORGANIZATION_GLOSSARIES}
        navigation={[
          [
            t('organization_glossaries_title'),
            LINKS.ORGANIZATION_GLOSSARIES.build({
              [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
            }),
          ],
        ]}
        loading={glossaries.isLoading}
        hideChildrenOnLoading={false}
        maxWidth={1000}
        allCentered
        onAdd={items && onCreate}
        addLabel={t('glossaries_add_button')}
      >
        {createDialogOpen && (
          <GlossaryCreateEditDialog
            open={createDialogOpen}
            onClose={() => setCreateDialogOpen(false)}
            onFinished={() => setCreateDialogOpen(false)}
            organizationId={organization!.id}
          />
        )}
        <PaginatedHateoasList
          wrapperComponentProps={{ className: 'listWrapper' }}
          onPageChange={setPage}
          loadable={glossaries}
          renderItem={(g) => (
            <GlossaryListItem
              key={g.id}
              glossary={g}
              organization={organization!}
            />
          )}
          emptyPlaceholder={
            <GlossariesEmptyListMessage
              loading={glossaries.isFetching}
              onCreateClick={onCreate}
            />
          }
        />
      </BaseOrganizationSettingsView>
    </StyledWrapper>
  );
};
