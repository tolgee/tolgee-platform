import {
  Box,
  Button,
  ButtonGroup,
  IconButton,
  Tooltip,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import {
  FileDownload03,
  LayoutGrid02,
  LayoutLeft,
  UploadCloud02,
} from '@untitled-ui/icons-react';
import { MultiselectItem } from 'tg.component/searchSelect/MultiselectItem';
import { InfiniteMultiSearchSelect } from 'tg.component/searchSelect/InfiniteMultiSearchSelect';
import { LanguageValue } from 'tg.component/languages/LanguageValue';
import { SecondaryBarSearchField } from 'tg.component/layout/SecondaryBarSearchField';
import { BaseViewAddButton } from 'tg.component/layout/BaseViewAddButton';
import { components } from 'tg.service/apiSchema.generated';
import { UseInfiniteQueryResult } from 'react-query';
import { ApiError } from 'tg.service/http/ApiError';
import { EntryRowLayout } from 'tg.ee.module/translationMemory/components/content/TranslationMemoryEntryRow';

type OrganizationLanguageModel =
  components['schemas']['OrganizationLanguageModel'];

type LanguagesLoadable = UseInfiniteQueryResult<
  components['schemas']['PagedModelOrganizationLanguageModel'],
  ApiError
>;

type Props = {
  search: string | undefined;
  onSearch: ((search: string) => void) | undefined;
  totalCount: number | undefined;
  layout: EntryRowLayout;
  onLayoutChange: (layout: EntryRowLayout) => void;
  languages: OrganizationLanguageModel[] | undefined;
  languagesLoadable: LanguagesLoadable;
  selectedLanguages: string[];
  langSearch: string;
  onLangSearchChange: (s: string) => void;
  onFetchMoreLanguages: () => void;
  onToggleLanguage: (item: OrganizationLanguageModel) => void;
  onClearLanguages: () => void;
  sourceLanguageTag: string;
  canManage: boolean;
  onImport: () => void;
  onExport: () => void;
  exportDisabled: boolean;
  onCreate: () => void;
  /**
   * When true, the search field, total-count chip, layout toggle and language filter are
   * hidden — used while the empty-state wizard is on screen so the toolbar collapses to
   * just the right-hand action group instead of presenting empty filter widgets.
   */
  hideFilters?: boolean;
};

export const TmEntriesToolbar: React.VFC<Props> = ({
  search,
  onSearch,
  totalCount,
  layout,
  onLayoutChange,
  languages,
  languagesLoadable,
  selectedLanguages,
  langSearch,
  onLangSearchChange,
  onFetchMoreLanguages,
  onToggleLanguage,
  onClearLanguages,
  sourceLanguageTag,
  canManage,
  onImport,
  onExport,
  exportDisabled,
  onCreate,
  hideFilters,
}) => {
  const { t } = useTranslate();
  const renderLangItem = (props: object, item: OrganizationLanguageModel) => {
    const isBase = item.tag === sourceLanguageTag;
    const selected = isBase || selectedLanguages.includes(item.tag);
    return (
      <MultiselectItem
        {...props}
        data-cy="tm-entries-language-select-item"
        disabled={isBase}
        selected={selected}
        label={<LanguageValue language={item} />}
        onClick={isBase ? undefined : () => onToggleLanguage(item)}
      />
    );
  };

  return (
    <Box display="flex" justifyContent="space-between" mb={2}>
      <Box display="flex" alignItems="center" gap="8px">
        {!hideFilters && (
          <>
            <SecondaryBarSearchField
              onSearch={onSearch}
              initial={search}
              placeholder={t(
                'translation_memory_entries_search_placeholder',
                'Search entries...'
              )}
            />
            {totalCount !== undefined && (
              <Typography
                variant="body2"
                color="text.secondary"
                data-cy="tm-entries-total-count"
              >
                <T
                  keyName="translation_memory_entries_total_count"
                  defaultValue="{count, plural, one {# entry} other {# entries}}"
                  params={{ count: totalCount }}
                />
              </Typography>
            )}
          </>
        )}
      </Box>
      <Box display="flex" gap={1} alignItems="center">
        {!hideFilters && (
          <>
            <ButtonGroup>
              <Tooltip
                title={t(
                  'translation_memory_layout_flat_tooltip',
                  'One row per entry'
                )}
              >
                <Button
                  color={
                    layout === 'flat' ? 'primary' : ('default' as 'primary')
                  }
                  onClick={() => onLayoutChange('flat')}
                  sx={{ px: 1 }}
                  data-cy="tm-entries-layout-flat"
                >
                  <LayoutGrid02 width={24} height={24} />
                </Button>
              </Tooltip>
              <Tooltip
                title={t(
                  'translation_memory_layout_stacked_tooltip',
                  'Stack languages vertically'
                )}
              >
                <Button
                  color={
                    layout === 'stacked' ? 'primary' : ('default' as 'primary')
                  }
                  onClick={() => onLayoutChange('stacked')}
                  sx={{ px: 1 }}
                  data-cy="tm-entries-layout-stacked"
                >
                  <LayoutLeft width={24} height={24} />
                </Button>
              </Tooltip>
            </ButtonGroup>
            <Box data-cy="tm-entries-language-filter" sx={{ width: 250 }}>
              <InfiniteMultiSearchSelect
                items={languages}
                selected={selectedLanguages}
                queryResult={languagesLoadable}
                itemKey={(item) => item.tag}
                search={langSearch}
                onClearSelected={onClearLanguages}
                onSearchChange={onLangSearchChange}
                onFetchMore={onFetchMoreLanguages}
                renderItem={renderLangItem}
                labelItem={(item) => item}
                searchPlaceholder={t(
                  'language_search_placeholder',
                  'Search languages'
                )}
                minHeight={false}
              />
            </Box>
          </>
        )}
        {canManage && (
          <Tooltip title={t('translation_memory_import_tmx', 'Import TMX')}>
            <IconButton
              size="small"
              color="primary"
              onClick={onImport}
              data-cy="tm-import-menu-button"
            >
              <UploadCloud02 width={20} height={20} />
            </IconButton>
          </Tooltip>
        )}
        <Tooltip title={t('translation_memory_export_tmx', 'Export TMX')}>
          <IconButton
            size="small"
            color="primary"
            onClick={onExport}
            disabled={exportDisabled}
            data-cy="tm-export-button"
          >
            <FileDownload03 width={20} height={20} />
          </IconButton>
        </Tooltip>
        {canManage && (
          <BaseViewAddButton
            onClick={onCreate}
            label={t('translation_memory_new_entry', 'Entry')}
          />
        )}
      </Box>
    </Box>
  );
};
