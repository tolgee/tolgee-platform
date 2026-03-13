import {
  Box,
  Button,
  CircularProgress,
  Popover,
  styled,
  Typography,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Link, useHistory } from 'react-router-dom';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { getProjectTranslationsUrl, LINKS, PARAMS } from 'tg.constants/links';
import { QaCheck } from 'tg.component/CustomIcons';
import { QaBadgePopoverProps } from '../../../eeSetup/EeModuleType';
import { useQaCheckTypeLabel } from 'tg.ee.module/qa/hooks/useQaCheckTypeLabel';

const StyledPopover = styled(Popover)`
  .MuiPaper-root {
    border-radius: 16px;
    box-shadow: 0px 2px 8px rgba(0, 0, 0, 0.2);
    min-width: 300px;
    max-width: 360px;
  }
`;

const StyledContent = styled(Box)`
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 16px;
`;

const StyledHeader = styled(Box)`
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 0 8px;
`;

const StyledRow = styled(Box)`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;

  &:hover {
    background: ${({ theme }) => theme.palette.tokens.text._states.hover};
  }

  & .show-link {
    visibility: hidden;
  }

  &:hover .show-link {
    visibility: visible;
  }
`;

const StyledButtons = styled(Box)`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px;
`;

type IssueRowProps = {
  label: string;
  count: number;
  onClick: () => void;
};

// TODO: move IssueRow to separate file
function IssueRow({ label, count, onClick }: IssueRowProps) {
  const { t } = useTranslate();
  return (
    <StyledRow onClick={onClick}>
      <Typography variant="body2">
        {label}: {count}
      </Typography>
      <Typography
        className="show-link"
        variant="button"
        sx={{
          // TODO: use `Styled*` component instead of this
          fontSize: 13,
          color: 'primary.main',
          cursor: 'pointer',
        }}
      >
        {t('qa_dashboard_popover_show')}
      </Typography>
    </StyledRow>
  );
}

export const QaBadgePopover = ({
  anchorEl,
  onClose,
  languageId,
  languageTag,
}: QaBadgePopoverProps) => {
  const { t } = useTranslate();
  const project = useProject();
  const history = useHistory();
  const languages = useProjectLanguages();
  const baseLanguage = languages.find((l) => l.base)?.tag;

  const { data, isLoading } = useApiQuery({
    url: '/v2/projects/{projectId}/stats/qa-issue-counts',
    method: 'get',
    path: { projectId: project.id },
    query: { languageId },
    options: {
      enabled: Boolean(anchorEl),
    },
  });

  const entries = data
    ? Object.entries(data)
        .filter(([, count]) => count > 0)
        .sort(([, a], [, b]) => b - a)
    : [];

  const navigateToTranslations = () => {
    const langs =
      languageTag === baseLanguage
        ? [languageTag]
        : [baseLanguage, languageTag].filter(Boolean);
    history.push(
      getProjectTranslationsUrl(project.id, {
        languages: langs as string[],
        filters: { filterHasQaIssues: true },
      })
    );
    onClose();
  };

  return (
    <StyledPopover
      open={Boolean(anchorEl)}
      anchorEl={anchorEl}
      onClose={onClose}
      anchorOrigin={{
        vertical: 'bottom',
        horizontal: 'center',
      }}
      transformOrigin={{
        vertical: 'top',
        horizontal: 'center',
      }}
    >
      <StyledContent>
        <StyledHeader>
          <QaCheck width={24} height={24} />
          <Typography variant="body2" fontWeight={500}>
            {t('qa_dashboard_popover_title')}
          </Typography>
        </StyledHeader>

        {isLoading ? (
          <Box display="flex" justifyContent="center" py={2}>
            <CircularProgress size={24} />
          </Box>
        ) : (
          <Box display="flex" flexDirection="column">
            {entries.map(([checkType, count]) => (
              <PopoverIssueRow
                key={checkType}
                checkType={checkType}
                count={count}
                onClick={navigateToTranslations}
              />
            ))}
            {entries.length === 0 && (
              <Box px={1}>
                <Typography variant="body2" color="text.secondary">
                  {t('qa_dashboard_popover_no_issues')}
                </Typography>
              </Box>
            )}
          </Box>
        )}

        <StyledButtons>
          <Typography
            component={Link}
            to={LINKS.PROJECT_EDIT_QA.build({
              [PARAMS.PROJECT_ID]: project.id,
            })}
            onClick={onClose}
            variant="button"
            sx={{
              fontSize: 13,
              color: 'text.secondary',
              textDecoration: 'none',
              cursor: 'pointer',
            }}
          >
            {t('qa_dashboard_popover_settings')}
          </Typography>
          <Button
            variant="outlined"
            color="primary"
            size="small"
            onClick={navigateToTranslations}
          >
            {t('qa_dashboard_popover_show_all')}
          </Button>
        </StyledButtons>
      </StyledContent>
    </StyledPopover>
  );
};

// TODO: merge into the IssueRow
function PopoverIssueRow({
  checkType,
  count,
  onClick,
}: {
  checkType: string;
  count: number;
  onClick: () => void;
}) {
  const label = useQaCheckTypeLabel(checkType as any);
  return <IssueRow label={label} count={count} onClick={onClick} />;
}
