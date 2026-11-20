import { useState } from 'react';
import {
  Badge,
  Divider,
  IconButton,
  Menu,
  MenuItem,
  Tooltip,
} from '@mui/material';
import { FilterLines } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';
import { CompactListSubheader } from 'tg.component/ListComponents';
import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { ActivityCategory } from './activityCategories';
import { useActivityCategoryLabels } from './useActivityCategoryLabels';
import { ActivityFilter, DEFAULT_ACTIVITY_FILTER } from './activityStreamQuery';

type Props = {
  value: ActivityFilter;
  onChange: (value: ActivityFilter) => void;
};

export const ActivityMiniFilter = ({ value, onChange }: Props) => {
  const { t } = useTranslate();
  const project = useProject();
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
  const categoryLabels = useActivityCategoryLabels();
  const members = useApiQuery({
    url: '/v2/projects/{projectId}/users',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
    options: { enabled: Boolean(anchorEl) },
  });
  const languages = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
    options: { enabled: Boolean(anchorEl) },
  });

  const isDefault =
    JSON.stringify(value) === JSON.stringify(DEFAULT_ACTIVITY_FILTER);
  const set = (patch: Partial<ActivityFilter>) => {
    onChange({ ...value, ...patch });
  };

  return (
    <>
      <Tooltip title={t('activity_filter_tooltip', 'Filter activity')}>
        <Badge
          color="primary"
          variant="dot"
          badgeContent={isDefault ? 0 : 1}
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
        onClose={() => setAnchorEl(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        slotProps={{ paper: { sx: { maxHeight: 520 } } }}
      >
        <CompactListSubheader sx={{ pt: 0 }}>
          <T keyName="activity_filter_people" defaultValue="People" />
        </CompactListSubheader>
        <MenuItem
          selected={value.user === 'ALL'}
          onClick={() => set({ user: 'ALL' })}
        >
          <T keyName="activity_stream_all_users" defaultValue="All users" />
        </MenuItem>
        <MenuItem
          selected={value.user === 'EXCEPT_ME'}
          onClick={() => set({ user: 'EXCEPT_ME' })}
        >
          <T
            keyName="activity_stream_except_me"
            defaultValue="Everyone except me"
          />
        </MenuItem>
        {members.data?._embedded?.users?.map((member) => (
          <MenuItem
            key={member.id}
            selected={value.user === String(member.id)}
            onClick={() => set({ user: String(member.id) })}
          >
            {member.name}
          </MenuItem>
        ))}
        <Divider />
        <CompactListSubheader>
          <T keyName="activity_filter_events" defaultValue="Events" />
        </CompactListSubheader>
        <MenuItem
          selected={value.category === 'ALL'}
          onClick={() => set({ category: 'ALL' })}
        >
          <T keyName="activity_filter_all_events" defaultValue="All events" />
        </MenuItem>
        {(Object.keys(categoryLabels) as ActivityCategory[]).map((c) => (
          <MenuItem
            key={c}
            selected={value.category === c}
            onClick={() => set({ category: c })}
          >
            {categoryLabels[c]}
          </MenuItem>
        ))}
        <Divider />
        <CompactListSubheader>
          <T keyName="activity_filter_languages" defaultValue="Languages" />
        </CompactListSubheader>
        <MenuItem
          selected={value.language === 'ALL'}
          onClick={() => set({ language: 'ALL' })}
        >
          <T
            keyName="activity_stream_all_languages"
            defaultValue="All languages"
          />
        </MenuItem>
        {languages.data?._embedded?.languages?.map((language) => (
          <MenuItem
            key={language.id}
            selected={value.language === String(language.id)}
            onClick={() => set({ language: String(language.id) })}
          >
            {language.flagEmoji} {language.name}
          </MenuItem>
        ))}
      </Menu>
    </>
  );
};
