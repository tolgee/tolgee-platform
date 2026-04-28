import { LINKS, PARAMS } from 'tg.constants/links';
import React, { useState } from 'react';
import { BaseOrganizationSettingsView } from 'tg.views/organizations/components/BaseOrganizationSettingsView';
import { useTranslate } from '@tolgee/react';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import {
  Box,
  InputAdornment,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  styled,
} from '@mui/material';
import { SearchSm } from '@untitled-ui/icons-react';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import {
  useEnabledFeatures,
  useIsOrganizationOwnerOrMaintainer,
} from 'tg.globalContext/helpers';
import { DisabledFeatureBanner } from 'tg.component/common/DisabledFeatureBanner';
import { TranslationMemoryListItem } from 'tg.ee.module/translationMemory/components/TranslationMemoryListItem';
import { TranslationMemoriesEmptyListMessage } from 'tg.ee.module/translationMemory/components/TranslationMemoriesEmptyListMessage';
import { TranslationMemoryCreateDialog } from 'tg.ee.module/translationMemory/views/TranslationMemoryCreateDialog';

const StyledWrapper = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;

  & .listWrapper > * > * + * {
    border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  }
`;

type TypeFilter = 'ALL' | 'SHARED' | 'PROJECT';

export const TranslationMemoriesListView = () => {
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [typeFilter, setTypeFilter] = useState<TypeFilter>('ALL');
  const [createDialogOpen, setCreateDialogOpen] = useState(false);

  const { isEnabled } = useEnabledFeatures();
  const featureEnabled = isEnabled('TRANSLATION_MEMORY');

  const organization = useOrganization();

  const { t } = useTranslate();

  const tms = useApiQuery({
    url: '/v2/organizations/{organizationId}/translation-memories-with-stats',
    method: 'get',
    path: { organizationId: organization!.id },
    query: {
      page,
      size: 20,
      search,
      type: typeFilter === 'ALL' ? undefined : typeFilter,
    },
    options: {
      enabled: featureEnabled,
      keepPreviousData: true,
    },
  });

  const items = tms?.data?._embedded?.translationMemories;

  const onCreate = () => {
    setCreateDialogOpen(true);
  };

  const canCreate = useIsOrganizationOwnerOrMaintainer();

  const isLoading = featureEnabled && tms.isLoading;
  const isEmpty =
    !isLoading && (items?.length ?? 0) === 0 && !search && typeFilter === 'ALL';

  const totalElements = tms.data?.page?.totalElements ?? 0;
  const hasActiveFilter = search !== '' || typeFilter !== 'ALL';
  // Keep the toolbar while filtering so the user can always clear the filter —
  // otherwise a filter that empties the list would hide its own off switch.
  const showToolbar = totalElements > 5 || hasActiveFilter;

  return (
    <StyledWrapper>
      <BaseOrganizationSettingsView
        windowTitle={t(
          'organization_translation_memories_title',
          'Translation memories'
        )}
        title={t(
          'organization_translation_memories_title',
          'Translation memories'
        )}
        link={LINKS.ORGANIZATION_TRANSLATION_MEMORIES}
        navigation={[
          [
            t(
              'organization_translation_memories_title',
              'Translation memories'
            ),
            LINKS.ORGANIZATION_TRANSLATION_MEMORIES.build({
              [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
            }),
          ],
        ]}
        loading={isLoading}
        hideChildrenOnLoading={false}
        maxWidth={1000}
        allCentered
        onAdd={canCreate && !isEmpty ? onCreate : undefined}
        addLabel={t('translation_memories_add_button', 'Translation memory')}
      >
        {canCreate && createDialogOpen && (
          <TranslationMemoryCreateDialog
            open={createDialogOpen}
            onClose={() => setCreateDialogOpen(false)}
            onFinished={() => setCreateDialogOpen(false)}
          />
        )}
        {featureEnabled ? (
          <>
            {isEmpty ? (
              <TranslationMemoriesEmptyListMessage
                loading={tms.isFetching}
                onCreateClick={canCreate ? onCreate : undefined}
              />
            ) : (
              <>
                {showToolbar && (
                  <Box display="flex" gap={2} alignItems="center" mb={2}>
                    <TextField
                      size="small"
                      placeholder={t(
                        'translation_memories_search_placeholder',
                        'Search...'
                      )}
                      value={search}
                      onChange={(e) => {
                        setSearch(e.target.value);
                        setPage(0);
                      }}
                      InputProps={{
                        startAdornment: (
                          <InputAdornment position="start">
                            <SearchSm width={18} height={18} />
                          </InputAdornment>
                        ),
                      }}
                      sx={{ minWidth: 180 }}
                    />
                    <ToggleButtonGroup
                      size="small"
                      sx={{
                        '& .MuiToggleButton-root': {
                          fontSize: 12,
                          px: 1.5,
                          py: 0.5,
                        },
                      }}
                      value={typeFilter}
                      exclusive
                      onChange={(_, val) => {
                        if (val !== null) {
                          setTypeFilter(val);
                          setPage(0);
                        }
                      }}
                    >
                      <ToggleButton value="ALL">
                        {t('translation_memories_filter_all', 'All')}
                      </ToggleButton>
                      <ToggleButton value="SHARED">
                        {t('translation_memories_filter_shared', 'Shared')}
                      </ToggleButton>
                      <ToggleButton value="PROJECT">
                        {t(
                          'translation_memories_filter_project',
                          'Project only'
                        )}
                      </ToggleButton>
                    </ToggleButtonGroup>
                  </Box>
                )}
                <PaginatedHateoasList
                  wrapperComponentProps={{ className: 'listWrapper' }}
                  onPageChange={setPage}
                  loadable={tms}
                  renderItem={(tm) => (
                    <TranslationMemoryListItem
                      key={tm.id}
                      translationMemory={tm}
                    />
                  )}
                  emptyPlaceholder={
                    <Box py={4} textAlign="center" color="text.secondary">
                      {t(
                        'translation_memories_no_results',
                        'No translation memories found.'
                      )}
                    </Box>
                  }
                />
              </>
            )}
          </>
        ) : (
          <Box>
            <DisabledFeatureBanner
              customMessage={t(
                'translation_memories_feature_description',
                'Translation memory management is available on the Business plan.'
              )}
            />
          </Box>
        )}
      </BaseOrganizationSettingsView>
    </StyledWrapper>
  );
};
