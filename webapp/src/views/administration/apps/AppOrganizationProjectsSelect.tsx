import {
  Box,
  Checkbox,
  CircularProgress,
  FormControlLabel,
  styled,
  Typography,
} from '@mui/material';
import { T } from '@tolgee/react';

export type SelectableProject = {
  id: number;
  name: string;
};

const StyledList = styled('div')`
  display: grid;
  max-height: 240px;
  overflow-y: auto;
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  border: 1px solid ${({ theme }) => theme.palette.divider};
  padding: ${({ theme }) => theme.spacing(0.5, 1)};
`;

const StyledEmpty = styled('div')`
  padding: ${({ theme }) => theme.spacing(2)};
  color: ${({ theme }) => theme.palette.text.secondary};
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  border: 1px solid ${({ theme }) => theme.palette.divider};
`;

type Props = {
  projects: SelectableProject[];
  loading: boolean;
  truncated: boolean;
  selectedIds: number[];
  disabled?: boolean;
  onChange: (ids: number[]) => void;
};

export const AppOrganizationProjectsSelect = ({
  projects,
  loading,
  truncated,
  selectedIds,
  disabled,
  onChange,
}: Props) => {
  if (loading) {
    return (
      <Box display="flex" justifyContent="center" py={2}>
        <CircularProgress size={20} />
      </Box>
    );
  }

  if (projects.length === 0) {
    return (
      <StyledEmpty data-cy="administration-apps-projects-empty">
        <Typography variant="body2">
          <T
            keyName="administration_apps_projects_empty"
            defaultValue="This organization has no projects yet. You can still grant it access — its projects can enable the app later."
          />
        </Typography>
      </StyledEmpty>
    );
  }

  const selected = new Set(selectedIds);
  const allSelected = projects.every((project) => selected.has(project.id));

  const toggle = (projectId: number) => {
    if (selected.has(projectId)) {
      onChange(selectedIds.filter((id) => id !== projectId));
      return;
    }
    onChange([...selectedIds, projectId]);
  };

  const toggleAll = () => {
    if (allSelected) {
      onChange([]);
      return;
    }
    onChange(projects.map((project) => project.id));
  };

  return (
    <>
      <StyledList data-cy="administration-apps-projects-select">
        <FormControlLabel
          control={
            <Checkbox
              size="small"
              checked={allSelected}
              indeterminate={!allSelected && selectedIds.length > 0}
              disabled={disabled}
              onChange={toggleAll}
              data-cy="administration-apps-projects-select-all"
            />
          }
          label={
            <Typography variant="body2" color="text.secondary">
              <T
                keyName="administration_apps_projects_select_all"
                defaultValue="Select all"
              />
            </Typography>
          }
        />
        {projects.map((project) => (
          <FormControlLabel
            key={project.id}
            control={
              <Checkbox
                size="small"
                checked={selected.has(project.id)}
                disabled={disabled}
                onChange={() => toggle(project.id)}
                data-cy="administration-apps-projects-item"
                data-cy-project-id={project.id}
              />
            }
            label={<Typography variant="body2">{project.name}</Typography>}
          />
        ))}
      </StyledList>
      {truncated && (
        <Typography
          variant="caption"
          color="text.secondary"
          data-cy="administration-apps-projects-truncated"
        >
          <T
            keyName="administration_apps_projects_truncated"
            defaultValue="Only the first {count} projects are listed. Enable the app for the rest from their project settings."
            params={{ count: projects.length }}
          />
        </Typography>
      )}
    </>
  );
};
