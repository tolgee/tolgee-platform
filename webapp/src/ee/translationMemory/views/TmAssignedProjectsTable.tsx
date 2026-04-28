import {
  Box,
  Chip,
  IconButton,
  Switch,
  Tooltip,
  Typography,
  styled,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { RefreshCcw01, XClose } from '@untitled-ui/icons-react';
import { PenaltyCell } from 'tg.ee.module/translationMemory/components/PenaltyCell';

const StyledProjectTable = styled('div')`
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-radius: 8px;
  overflow: hidden;
  margin-top: 8px;
`;

const StyledProjectHeader = styled('div')`
  display: grid;
  grid-template-columns: 1fr 110px 60px 60px 36px;
  padding: ${({ theme }) => theme.spacing(0.75, 2)};
  font-size: 11px;
  text-transform: uppercase;
  color: ${({ theme }) => theme.palette.text.secondary};
  letter-spacing: 0.04em;
  font-weight: 500;
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
`;

const StyledProjectRow = styled('div')`
  display: grid;
  grid-template-columns: 1fr 110px 60px 60px 36px;
  align-items: center;
  padding: ${({ theme }) => theme.spacing(1, 2)};
  border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  &:first-of-type {
    border-top: none;
  }
`;

const StyledMarkedRow = styled('div')`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: ${({ theme }) => theme.spacing(1, 2)};
  border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  background: ${({ theme }) => theme.palette.error.light}11;
  &:first-of-type {
    border-top: none;
  }
`;

const StyledMarkedName = styled(Typography)`
  text-decoration: line-through;
  color: ${({ theme }) => theme.palette.text.disabled};
`;

export type AssignedProjectRow = {
  projectId: number;
  projectName: string;
  readAccess: boolean;
  writeAccess: boolean;
  penalty: number | null;
};

export type PendingRemovalRow = {
  projectId: number;
  projectName: string;
  keepData: boolean;
};

type Props = {
  rows: AssignedProjectRow[];
  removedRows: PendingRemovalRow[];
  defaultPenalty: number;
  onTogglePenalty: (projectId: number, value: number | null) => void;
  onToggleAccess: (
    projectId: number,
    field: 'readAccess' | 'writeAccess'
  ) => void;
  onRemove: (projectId: number, projectName: string) => void;
  onUndoRemove: (projectId: number) => void;
};

export const TmAssignedProjectsTable: React.VFC<Props> = ({
  rows,
  removedRows,
  defaultPenalty,
  onTogglePenalty,
  onToggleAccess,
  onRemove,
  onUndoRemove,
}) => {
  const { t } = useTranslate();

  if (rows.length === 0 && removedRows.length === 0) return null;

  return (
    <StyledProjectTable>
      <StyledProjectHeader>
        <div>{t('translation_memory_settings_col_project', 'Project')}</div>
        <div style={{ textAlign: 'center' }}>
          {t('translation_memory_settings_col_penalty', 'Penalty')}
        </div>
        <div style={{ textAlign: 'center' }}>
          {t('translation_memory_settings_col_read', 'Read')}
        </div>
        <div style={{ textAlign: 'center' }}>
          {t('translation_memory_settings_col_write', 'Write')}
        </div>
        <div />
      </StyledProjectHeader>
      {rows.map((row) => (
        <StyledProjectRow key={row.projectId}>
          <Typography variant="body2">{row.projectName}</Typography>
          <Box textAlign="center">
            <PenaltyCell
              defaultPenalty={defaultPenalty}
              override={row.penalty}
              onChange={(v) => onTogglePenalty(row.projectId, v)}
            />
          </Box>
          <Box textAlign="center">
            <Switch
              size="small"
              checked={row.readAccess}
              onChange={() => onToggleAccess(row.projectId, 'readAccess')}
            />
          </Box>
          <Box textAlign="center">
            <Switch
              size="small"
              checked={row.writeAccess}
              onChange={() => onToggleAccess(row.projectId, 'writeAccess')}
            />
          </Box>
          <Box textAlign="center">
            <IconButton
              size="small"
              onClick={() => onRemove(row.projectId, row.projectName)}
            >
              <XClose width={14} height={14} />
            </IconButton>
          </Box>
        </StyledProjectRow>
      ))}
      {removedRows.map((row) => (
        <StyledMarkedRow key={row.projectId}>
          <StyledMarkedName variant="body2">{row.projectName}</StyledMarkedName>
          <Chip
            size="small"
            label={
              row.keepData
                ? t('tm_settings_keep_data_badge', 'Keep data')
                : t('tm_settings_discard_data_badge', 'Discard data')
            }
            color={row.keepData ? 'info' : 'default'}
            variant="outlined"
            sx={{ flexShrink: 0 }}
          />
          <Box sx={{ ml: 'auto' }}>
            <Tooltip title={t('tm_settings_undo_remove', 'Undo')}>
              <IconButton
                size="small"
                onClick={() => onUndoRemove(row.projectId)}
              >
                <RefreshCcw01 width={14} height={14} />
              </IconButton>
            </Tooltip>
          </Box>
        </StyledMarkedRow>
      ))}
    </StyledProjectTable>
  );
};
