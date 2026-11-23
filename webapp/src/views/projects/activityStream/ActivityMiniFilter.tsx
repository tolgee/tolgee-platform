import { useState } from 'react';
import { Badge, Box, Divider, IconButton, Menu, Tooltip } from '@mui/material';
import { FilterLines } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';
import { SubmenuItem } from 'tg.component/SubmenuItem';
import { CompactMenuItem } from 'tg.component/ListComponents';
import { useLanguagesContent } from 'tg.component/common/form/LanguagesSelect/useLanguagesContent';
import {
  ActivityFilter,
  DEFAULT_ACTIVITY_FILTER,
  useActivityFilter,
} from './activityStreamQuery';
import {
  activityFilterOptions,
  activityTreeOptions,
} from './ActivityFilterOptions';

const SUBMENU_MAX_WIDTH = 400;

type SubmenuKey = 'people' | 'events' | 'languages';

type Props = {
  value: ActivityFilter;
  onChange: (value: ActivityFilter) => void;
  filter: ReturnType<typeof useActivityFilter>;
};

export const ActivityMiniFilter = ({ value, onChange, filter }: Props) => {
  const { t } = useTranslate();
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
  const [submenu, setSubmenu] = useState<SubmenuKey>();
  const [submenuPos, setSubmenuPos] = useState<{
    top: number;
    left: number;
    side: 'left' | 'right';
  }>({ top: 0, left: 0, side: 'left' });

  const languagesContent = useLanguagesContent({
    ...filter.languages,
    disabledLanguages: undefined,
    enableEmpty: true,
    selectAllToggle: true,
  });

  const openSubmenu =
    (key: SubmenuKey) => (e: React.MouseEvent<HTMLElement>) => {
      const item = e.currentTarget;
      const paper = item.closest<HTMLElement>('.MuiPaper-root');
      const paperLeft = parseFloat(paper?.style.left ?? '') || 0;
      const paperTop = parseFloat(paper?.style.top ?? '') || 0;
      const left = paperLeft;
      const right = paperLeft + (paper?.offsetWidth ?? 0);
      const rect = {
        left,
        right,
        top: paperTop + item.offsetTop - (paper?.scrollTop ?? 0),
      };
      const fitsRight =
        window.innerWidth - rect.right >= SUBMENU_MAX_WIDTH + 16;
      setSubmenuPos({
        top: rect.top,
        left: fitsRight ? rect.right : rect.left,
        side: fitsRight ? 'left' : 'right',
      });
      setSubmenu(key);
    };

  const submenuProps = (key: SubmenuKey) => ({
    open: submenu === key,
    anchorReference: 'anchorPosition' as const,
    anchorPosition: { top: submenuPos.top, left: submenuPos.left },
    onClose: () => setSubmenu(undefined),
    transformOrigin: { vertical: 'top' as const, horizontal: submenuPos.side },
    slotProps: {
      paper: {
        sx: { maxHeight: 420, minWidth: 220, maxWidth: SUBMENU_MAX_WIDTH },
      },
    },
  });

  return (
    <>
      <Tooltip title={t('activity_filter_tooltip', 'Filter activity')}>
        <Badge
          color="primary"
          variant="dot"
          invisible={!filter.isActive}
          overlap="circular"
        >
          <IconButton
            onClick={(e) => setAnchorEl(e.currentTarget)}
            data-cy="activity-mini-filter-button"
          >
            <FilterLines />
          </IconButton>
        </Badge>
      </Tooltip>
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => {
          setAnchorEl(null);
          setSubmenu(undefined);
        }}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Box display="grid" minWidth={220}>
          <SubmenuItem
            label={<T keyName="activity_filter_people" defaultValue="People" />}
            open={submenu === 'people'}
            selected={Boolean(value.users.length)}
            onClick={openSubmenu('people')}
          />
          <SubmenuItem
            label={<T keyName="activity_filter_events" defaultValue="Events" />}
            open={submenu === 'events'}
            selected={Boolean(value.activities.length)}
            onClick={openSubmenu('events')}
          />
          <SubmenuItem
            label={
              <T keyName="activity_filter_languages" defaultValue="Languages" />
            }
            open={submenu === 'languages'}
            selected={Boolean(value.languages.length)}
            onClick={openSubmenu('languages')}
          />
          {filter.isActive && [
            <Divider key="divider" />,
            <CompactMenuItem
              key="clear"
              onClick={() => onChange(DEFAULT_ACTIVITY_FILTER)}
              data-cy="activity-mini-filter-clear"
            >
              <T keyName="activity_filter_clear" defaultValue="Clear filters" />
            </CompactMenuItem>,
          ]}
        </Box>
      </Menu>

      <Menu {...submenuProps('people')}>
        {activityFilterOptions(filter.users)}
      </Menu>
      <Menu {...submenuProps('events')}>
        {activityTreeOptions(filter.activities)}
      </Menu>
      <Menu {...submenuProps('languages')}>{languagesContent}</Menu>
    </>
  );
};
