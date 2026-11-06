import { useState } from 'react';
import { Badge, Box, IconButton, Tooltip, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { SearchSm, XClose } from '@untitled-ui/icons-react';

import { Sort } from 'tg.component/CustomIcons';
import { SecondaryBarSearchField } from 'tg.component/layout/SecondaryBarSearchField';
import { TranslationSortMenu } from 'tg.component/translation/translationSort/TranslationSortMenu';

export type SortValue = 'suggested' | 'contributions' | 'accepted' | 'recent';

export const StyledSectionTitle = styled('h2')`
  margin: 0;
  font-size: ${({ theme }) => theme.typography.body2.fontSize}px;
  font-weight: 500;
  text-transform: uppercase;
  white-space: nowrap;
`;

type Props = {
  isSmall: boolean;
  search: string;
  onSearch: (value: string) => void;
  showSearch: boolean;
  showControls: boolean;
  sort: SortValue;
  onSortChange: (value: string) => void;
};

export const ContributorsToolbar = ({
  isSmall,
  search,
  onSearch,
  showSearch,
  showControls,
  sort,
  onSortChange,
}: Props) => {
  const { t } = useTranslate();
  const [sortAnchor, setSortAnchor] = useState<HTMLElement | null>(null);
  const [searchOpen, setSearchOpen] = useState(Boolean(search));

  const sortOptions = [
    {
      value: 'suggested',
      label: t('community_sort_suggested', 'Most suggestions'),
    },
    { value: 'accepted', label: t('community_sort_accepted', 'Most accepted') },
    { value: 'recent', label: t('community_sort_recent', 'Recently active') },
    {
      value: 'contributions',
      label: t('community_sort_contributions', 'Most contributions'),
    },
  ];

  return (
    <Box display="flex" alignItems="center" gap={isSmall ? 1 : 2}>
      {isSmall && searchOpen ? (
        <>
          <SecondaryBarSearchField
            autoFocus
            initial={search}
            onSearch={onSearch}
            sx={{ flexGrow: 1, width: 'auto' }}
            data-cy="community-contributors-search"
          />
          <IconButton
            onClick={() => {
              setSearchOpen(false);
              onSearch('');
            }}
            data-cy="community-contributors-search-close"
          >
            <XClose />
          </IconButton>
        </>
      ) : (
        <StyledSectionTitle
          sx={{ flexGrow: isSmall ? 1 : 0 }}
          data-cy="community-contributors-section-title"
        >
          {t('community_contributors_section_title', 'Contributors')}
        </StyledSectionTitle>
      )}
      <Box
        display={(isSmall && searchOpen) || !showControls ? 'none' : 'flex'}
        alignItems="center"
        gap={1}
      >
        {!showSearch ? null : isSmall ? (
          <IconButton
            onClick={() => setSearchOpen(true)}
            data-cy="community-contributors-search-open"
          >
            <SearchSm />
          </IconButton>
        ) : (
          <Box>
            <SecondaryBarSearchField
              initial={search}
              onSearch={onSearch}
              data-cy="community-contributors-search"
            />
          </Box>
        )}
        <Tooltip title={t('translation_controls_sort_tooltip')}>
          <Badge
            color="primary"
            variant="dot"
            badgeContent={sort === 'suggested' ? 0 : 1}
            overlap="circular"
          >
            <IconButton
              onClick={(e) => setSortAnchor(e.currentTarget)}
              data-cy="community-contributors-sort-button"
            >
              <Sort />
            </IconButton>
          </Badge>
        </Tooltip>
        <TranslationSortMenu
          value={sort}
          options={sortOptions}
          onChange={onSortChange}
          anchorEl={sortAnchor}
          onClose={() => setSortAnchor(null)}
        />
      </Box>
    </Box>
  );
};
