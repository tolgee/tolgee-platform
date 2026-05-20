import { Chip, IconButton, styled, Tooltip, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Settings01, DotsGrid } from '@untitled-ui/icons-react';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { components } from 'tg.service/apiSchema.generated';

type TranslateFn = ReturnType<typeof useTranslate>['t'];

const mutedChipSx = (theme: any) => ({
  flexShrink: 0,
  backgroundColor: theme.palette.placeholders.variant.background,
  color: theme.palette.placeholders.variant.text,
  border: `1px solid ${theme.palette.placeholders.variant.border}`,
});

/**
 * Renders the access-state badges for a shared TM assignment. Both R+W → two side-by-side
 * "Read" / "Write" chips. Only one of them → a single "Read-only" / "Write-only" chip
 * that spells out the effective state. Neither → a single "No access" chip. All variants
 * use the muted palette so they don't compete visually with the type chip.
 */
function accessChips(read: boolean, write: boolean, t: TranslateFn) {
  if (read && write) {
    return (
      <>
        <Chip
          size="small"
          label={t('project_settings_tm_access_read', 'Read')}
          sx={mutedChipSx}
        />
        <Chip
          size="small"
          label={t('project_settings_tm_access_write', 'Write')}
          sx={mutedChipSx}
        />
      </>
    );
  }
  if (read) {
    return (
      <Chip
        size="small"
        label={t('project_settings_tm_access_read_only', 'Read-only')}
        sx={mutedChipSx}
      />
    );
  }
  if (write) {
    return (
      <Chip
        size="small"
        label={t('project_settings_tm_access_write_only', 'Write-only')}
        sx={mutedChipSx}
      />
    );
  }
  // No badge for the no-access case — the absence is the signal. A subtle italic label
  // keeps the row readable without the visual weight of a chip border.
  return (
    <Typography variant="body2" color="text.disabled" fontStyle="italic">
      {t('project_settings_tm_access_none', 'No access')}
    </Typography>
  );
}

type AssignmentModel =
  components['schemas']['ProjectTranslationMemoryAssignmentModel'];

const StyledRow = styled('div')`
  display: grid;
  grid-template-columns: 28px 40px 1fr auto 36px;
  align-items: center;
  gap: 8px;
  padding: ${({ theme }) => theme.spacing(1.25, 1.5)};
  border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  background: ${({ theme }) => theme.palette.background.default};
  &:first-of-type {
    border-top: none;
  }
`;

const StyledDragHandle = styled('div')`
  display: flex;
  align-items: center;
  color: ${({ theme }) => theme.palette.text.disabled};
  cursor: grab;
  touch-action: none;
`;

const StyledName = styled('div')`
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
`;

const StyledBadges = styled('div')`
  display: flex;
  gap: 4px;
  align-items: center;
`;

type Props = {
  tm: AssignmentModel;
  index: number;
  onSettings: () => void;
  canEdit: boolean;
};

export const SortableTmRow = ({ tm, index, onSettings, canEdit }: Props) => {
  const { t } = useTranslate();
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: tm.translationMemoryId });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    zIndex: isDragging ? 1 : undefined,
    opacity: isDragging ? 0.8 : undefined,
    boxShadow: isDragging ? '0 2px 8px rgba(0,0,0,0.15)' : undefined,
  };

  return (
    <StyledRow ref={setNodeRef} style={style} data-cy="project-settings-tm-row">
      {canEdit ? (
        <StyledDragHandle {...attributes} {...listeners}>
          <DotsGrid width={16} height={16} />
        </StyledDragHandle>
      ) : (
        <div />
      )}
      <Typography variant="body2" color="text.secondary" fontWeight={600}>
        {index + 1}
      </Typography>
      <StyledName>
        {tm.translationMemoryName}
        <Chip
          size="small"
          label={
            tm.type === 'PROJECT'
              ? t('translation_memory_type_project_only', 'Project only')
              : t('translation_memory_type_shared', 'Shared')
          }
          color={tm.type === 'PROJECT' ? undefined : 'primary'}
          sx={(theme) => ({
            flexShrink: 0,
            ...(tm.type === 'PROJECT'
              ? {
                  backgroundColor:
                    theme.palette.placeholders.variant.background,
                  color: theme.palette.placeholders.variant.text,
                  border: `1px solid ${theme.palette.placeholders.variant.border}`,
                }
              : {}),
          })}
        />
      </StyledName>
      <StyledBadges>
        {tm.type === 'PROJECT' ? (
          <Tooltip
            title={t(
              'project_settings_tm_always_on_tooltip',
              'The project’s own TM is always read and written — it is the implicit translation memory built from this project’s translations. Read/write access can be configured per project only on shared TMs.'
            )}
          >
            <Chip
              size="small"
              color="default"
              variant="outlined"
              label={t('project_settings_tm_always_on', 'Always on')}
            />
          </Tooltip>
        ) : (
          accessChips(tm.readAccess, tm.writeAccess, t)
        )}
      </StyledBadges>
      {canEdit && (
        <Tooltip title={t('project_settings_tm_settings', 'Settings')}>
          <IconButton
            size="small"
            onClick={onSettings}
            aria-label={t('project_settings_tm_settings', 'Settings')}
          >
            <Settings01 width={16} height={16} />
          </IconButton>
        </Tooltip>
      )}
    </StyledRow>
  );
};
