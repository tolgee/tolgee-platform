import { useState } from 'react';
import { Badge, Box, IconButton, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { Sort } from 'tg.component/CustomIcons';
import SearchField from 'tg.component/common/form/fields/SearchField';
import { TranslationSortMenu } from 'tg.component/translation/translationSort/TranslationSortMenu';
import { CommunityLanguageSort } from 'tg.views/projects/community/useCommunityLanguageList';

type Props = {
  search: string;
  onSearch: (search: string) => void;
  sort: CommunityLanguageSort;
  onSort: (sort: CommunityLanguageSort) => void;
};

export const CommunityLanguagesToolbar = ({
  search,
  onSearch,
  sort,
  onSort,
}: Props) => {
  const { t } = useTranslate();
  const stickyTop = useGlobalContext(
    (c) => c.layout.topBarHeight + c.layout.topBannerHeight
  );
  const [sortAnchor, setSortAnchor] = useState<HTMLElement | null>(null);

  const sortOptions = [
    { value: 'name', label: t('community_languages_sort_name', 'A to Z') },
    {
      value: 'nameDesc',
      label: t('community_languages_sort_name_desc', 'Z to A'),
    },
    {
      value: 'workLeft',
      label: t('community_languages_sort_work_left', 'Most work left'),
    },
    { value: 'done', label: t('community_languages_sort_done', 'Most done') },
  ];

  return (
    <Box
      display="flex"
      alignItems="center"
      gap={1}
      position="sticky"
      top={stickyTop}
      zIndex={2}
      py={1}
      my={-1}
      bgcolor="background.default"
      data-cy="community-languages-toolbar"
    >
      <SearchField
        initial={search}
        onSearch={onSearch}
        variant="outlined"
        size="small"
        placeholder={t('community_languages_search', 'Search languages')}
        sx={{ width: { xs: '100%', sm: 250 } }}
      />
      <Tooltip title={t('community_languages_sort_tooltip', 'Sort languages')}>
        <Badge
          color="primary"
          variant="dot"
          badgeContent={sort === 'name' ? 0 : 1}
          overlap="circular"
        >
          <IconButton
            onClick={(e) => setSortAnchor(e.currentTarget)}
            data-cy="community-languages-sort-button"
          >
            <Sort />
          </IconButton>
        </Badge>
      </Tooltip>
      <TranslationSortMenu
        value={sort}
        options={sortOptions}
        onChange={(value) => onSort(value as CommunityLanguageSort)}
        anchorEl={sortAnchor}
        onClose={() => setSortAnchor(null)}
      />
    </Box>
  );
};
