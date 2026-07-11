import { ArrowDropDown } from 'tg.component/CustomIcons';
import { XClose } from '@untitled-ui/icons-react';
import { Box, IconButton, styled, SxProps, Tooltip } from '@mui/material';
import { useRef, useState } from 'react';
import { TextField } from 'tg.component/common/TextField';
import { FakeInput } from 'tg.component/FakeInput';
import { TaskFilterPopover, TaskFilterType } from './TaskFilterPopover';
import { components } from 'tg.service/apiSchema.generated';
import { useTranslate } from '@tolgee/react';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { TaskTypeChip } from 'tg.component/task/TaskTypeChip';
import { filterEmpty } from './taskFilterUtils';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { useApiQuery, useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { AgencyLabel } from 'tg.ee';
import { useConfig } from 'tg.globalContext/helpers';

type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

const StyledInputButton = styled(IconButton)`
  margin: ${({ theme }) => theme.spacing(-1, -0.5, -1, -0.25)};
`;

const StyledContent = styled(Box)`
  display: flex;
  height: 100%;
  gap: 6px;
  align-items: center;
  & > * {
    flex-shrink: 0;
  }
`;

type Props = {
  value: TaskFilterType;
  onChange: (value: TaskFilterType) => void;
  project?: SimpleProjectModel;
  sx?: SxProps;
  className?: string;
};

const FilterTooltip = ({
  title,
  children,
}: {
  title: string | undefined;
  children: React.ReactNode;
}) => {
  return (
    <Tooltip title={title}>
      <Box display="flex" alignItems="center">
        {children}
      </Box>
    </Tooltip>
  );
};

export const TaskFilter = ({
  value,
  onChange,
  project,
  sx,
  className,
}: Props) => {
  const anchorEl = useRef(null);
  const [open, setOpen] = useState(false);
  const { t } = useTranslate();
  const config = useConfig();

  const usersLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/tasks/possible-assignees',
    method: 'get',
    path: { projectId: project?.id ?? 0 },
    query: { size: 10000, filterId: value.assignees },
    options: {
      enabled: Boolean(value.assignees?.length) && Boolean(project),
      keepPreviousData: true,
    },
  });

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project?.id ?? 0 },
    query: { size: 10000 },
    options: {
      enabled: Boolean(project),
      keepPreviousData: true,
    },
  });

  const projectsLoadable = useApiQuery({
    url: '/v2/projects',
    method: 'get',
    query: { size: 10000, filterId: value.projects },
    options: {
      enabled: !project && Boolean(value.projects?.length),
      keepPreviousData: true,
    },
  });

  const agenciesLoadable = useBillingApiQuery({
    url: '/v2/billing/translation-agency',
    method: 'get',
    query: {
      size: 1000,
    },
    options: {
      enabled: Boolean(value.agencies?.length) && config.billing.enabled,
      keepPreviousData: true,
    },
  });

  const languages = languagesLoadable.data?._embedded?.languages ?? [];

  function getFilterValue(value: TaskFilterType) {
    if (filterEmpty(value)) {
      return null;
    }

    return (
      <StyledContent>
        {usersLoadable.data?._embedded?.users
          ?.filter((u) => value.assignees?.includes(u.id))
          ?.map((user) => (
            <FilterTooltip title={user.name} key={user.id}>
              <AvatarImg
                owner={{
                  name: user.name,
                  avatar: user.avatar,
                  type: 'USER',
                  id: user.id,
                }}
                size={24}
              />
            </FilterTooltip>
          ))}
        {languages
          ?.filter((l) => value.languages?.includes(l.id))
          .map((language) => (
            <FilterTooltip
              title={`${language.name} (${language.tag})`}
              key={language.id}
            >
              <FlagImage flagEmoji={language.flagEmoji!} height={20} />
            </FilterTooltip>
          ))}
        {projectsLoadable.data?._embedded?.projects
          ?.filter((p) => value.projects?.includes(p.id))
          .map((project) => (
            <FilterTooltip title={project.name} key={project.id}>
              <AvatarImg
                owner={{
                  name: project.name,
                  avatar: project.avatar,
                  type: 'PROJECT',
                  id: project.id,
                }}
                size={24}
              />
            </FilterTooltip>
          ))}
        {agenciesLoadable.data?._embedded?.translationAgencies
          ?.filter((a) => value.agencies?.includes(a.id))
          .map((agency) => (
            <AgencyLabel key={agency.id} agency={agency} />
          ))}
        {value.types?.map((type) => (
          <TaskTypeChip key={type} type={type} />
        ))}
      </StyledContent>
    );
  }

  function handleClick() {
    setOpen(true);
  }

  return (
    <>
      <TextField
        variant="outlined"
        value={getFilterValue(value)}
        data-cy="tasks-header-filter-select"
        minHeight={false}
        placeholder={t('task_filter_placeholder')}
        InputProps={{
          onClick: handleClick,
          ref: anchorEl,
          fullWidth: true,
          sx: {
            cursor: 'pointer',
          },
          readOnly: true,
          inputComponent: FakeInput,
          margin: 'dense',
          endAdornment: (
            <Box sx={{ display: 'flex', marginRight: -0.5 }}>
              {!filterEmpty(value) && (
                <StyledInputButton
                  size="small"
                  onClick={stopBubble(() => onChange({}))}
                  tabIndex={-1}
                >
                  <XClose />
                </StyledInputButton>
              )}
              <StyledInputButton
                size="small"
                onClick={handleClick}
                tabIndex={-1}
                sx={{ pointerEvents: 'none' }}
              >
                <ArrowDropDown />
              </StyledInputButton>
            </Box>
          ),
        }}
        {...{ sx, className }}
      />
      {open && (
        <TaskFilterPopover
          open={open}
          onClose={() => setOpen(false)}
          value={value}
          onChange={onChange}
          anchorEl={anchorEl.current!}
          project={project}
          languages={languages}
        />
      )}
    </>
  );
};
