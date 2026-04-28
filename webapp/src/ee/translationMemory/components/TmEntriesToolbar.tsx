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
import { EntryRowLayout } from './TranslationMemoryEntryRow';

type OrganizationLanguageModel =
  components['schemas']['OrganizationLanguageModel'];

type Props = {
  search: string | undefined;
  onSearch: ((search: string) => void) | undefined;
  totalCount: number | undefined;
  layout: EntryRowLayout;
  onLayoutChange: (layout: EntryRowLayout) => void;
  languages: OrganizationLanguageModel[] | undefined;
  languagesLoadable: any;
  isAllSelected: boolean;
  selectedLanguages: string[] | undefined;
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
};

export const TmEntriesToolbar: React.VFC<Props> = ({
  search,
  onSearch,
  totalCount,
  layout,
  onLayoutChange,
  languages,
  languagesLoadable,
  isAllSelected,
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
}) => {
  const { t } = useTranslate();

  const renderLangItem = (props: object, item: OrganizationLanguageModel) => {
    const isBase = item.tag === sourceLanguageTag;
    const selected =
      isBase ||
      isAllSelected ||
      (selectedLanguages?.includes(item.tag) ?? false);
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
      </Box>
      <Box display="flex" gap={1} alignItems="center">
        <ButtonGroup>
          <Tooltip
            title={t(
              'translation_memory_layout_flat_tooltip',
              'One row per entry'
            )}
          >
            <Button
              color={layout === 'flat' ? 'primary' : ('default' as 'primary')}
              onClick={() => onLayoutChange('flat')}
              sx={{ px: 1 }}
              data-cy="tm-entries-layout-flat"
            >
              <LayoutGrid02 width={20} height={20} />
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
              <LayoutLeft width={20} height={20} />
            </Button>
          </Tooltip>
        </ButtonGroup>
        <Box data-cy="tm-entries-language-filter">
          <InfiniteMultiSearchSelect
            items={languages}
            selected={
              isAllSelected
                ? (languages ?? []).map((l) => l.tag)
                : selectedLanguages
            }
            queryResult={languagesLoadable}
            itemKey={(item) => item.tag}
            search={langSearch}
            onClearSelected={onClearLanguages}
            onSearchChange={onLangSearchChange}
            onFetchMore={onFetchMoreLanguages}
            renderItem={renderLangItem}
            labelItem={(item) => item}
            searchPlaceholder={t('language_search_placeholder')}
            minHeight={false}
          />
        </Box>
        {canManage && (
          <Tooltip title={t('translation_memory_import_tmx', 'Import TMX')}>
            <IconButton
              size="small"
              color="primary"
              onClick={onImport}
              data-cy="tm-import-button"
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
