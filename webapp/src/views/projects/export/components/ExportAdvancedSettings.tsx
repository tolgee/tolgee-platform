import { useEffect } from 'react';
import { useFormikContext } from 'formik';
import { Box, IconButton, styled } from '@mui/material';
import { ChevronDown, ChevronUp } from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';

import { getFormatById } from 'tg.views/projects/export/components/formatGroups';
import { StateSelector } from 'tg.views/projects/export/components/StateSelector';
import { NsSelector } from 'tg.views/projects/export/components/NsSelector';
import { MessageFormatSelector } from 'tg.views/projects/export/components/MessageFormatSelector';
import { SupportArraysSelector } from 'tg.views/projects/export/components/SupportArraysSelector';
import { EscapeHtmlSelector } from 'tg.views/projects/export/components/EscapeHtmlSelector';
import { TagsSelector } from 'tg.views/projects/export/components/TagsSelector';
import { ExportFormValues } from 'tg.views/projects/export/exportSettings';
import {
  SummaryItem,
  useAdvancedSettingsSummary,
} from 'tg.views/projects/export/components/useAdvancedSettingsSummary';

const StyledHeader = styled('div')`
  display: flex;
  min-width: 0;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1.5)};
  min-height: 40px;
  cursor: pointer;
`;

const StyledTitle = styled('div')`
  font-size: ${({ theme }) => theme.typography.subtitle2.fontSize}px;
  font-weight: ${({ theme }) => theme.typography.subtitle2.fontWeight};
  color: ${({ theme }) => theme.palette.text.primary};
  flex-shrink: 0;
`;

const StyledSummary = styled('div')`
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 14px;
  color: ${({ theme }) => theme.palette.text.secondary};

  & > span + span::before {
    content: '·';
    margin: ${({ theme }) => theme.spacing(0, 0.75)};
    color: ${({ theme }) => theme.palette.text.secondary};
    font-weight: 400;
  }

  & > .changed {
    color: ${({ theme }) => theme.palette.text.primary};
    font-weight: 500;
  }

  & > .invalid {
    color: ${({ theme }) => theme.palette.error.main};
    font-weight: 500;
  }
`;

const StyledContent = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(2)};
`;

const StyledRow = styled('div')`
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: ${({ theme }) => theme.spacing(2, 3)};

  &:empty {
    display: none;
  }
`;

type Props = {
  allNamespaces: string[] | undefined;
  collapsible?: boolean;
  expanded?: boolean;
  onExpandedChange?: (
    expanded: boolean,
    source: 'user' | 'invalidField'
  ) => void;
};

export const ExportAdvancedSettings = ({
  allNamespaces,
  collapsible = false,
  expanded = true,
  onExpandedChange,
}: Props) => {
  const { values, errors } = useFormikContext<ExportFormValues>();
  const summary = useAdvancedSettingsSummary(values, allNamespaces);
  const hasInvalidField = Boolean(errors.states);

  useEffect(() => {
    if (collapsible && !expanded && hasInvalidField) {
      onExpandedChange?.(true, 'invalidField');
    }
  }, [collapsible, expanded, hasInvalidField]);

  const format = getFormatById(values.format);
  const content = (
    <StyledContent data-cy="export-advanced-settings-content">
      <StyledRow>
        <StateSelector />
        <NsSelector namespaces={allNamespaces} />
      </StyledRow>
      <StyledRow>
        <MessageFormatSelector />
      </StyledRow>
      {(format.showSupportArrays || format.showEscapeHtml) && (
        <Box display="grid" justifyItems="start">
          {format.showSupportArrays && <SupportArraysSelector />}
          {format.showEscapeHtml && <EscapeHtmlSelector />}
        </Box>
      )}
      <TagsSelector />
    </StyledContent>
  );

  if (!collapsible) {
    return content;
  }

  return (
    <Box
      display="grid"
      gridTemplateColumns="minmax(0, 1fr)"
      gap={1}
      data-cy="export-advanced-settings"
    >
      <AdvancedSettingsHeader
        expanded={expanded}
        summary={summary}
        onToggle={() => onExpandedChange?.(!expanded, 'user')}
      />
      {expanded && content}
    </Box>
  );
};

const AdvancedSettingsHeader = ({
  expanded,
  summary,
  onToggle,
}: {
  expanded: boolean;
  summary: SummaryItem[];
  onToggle: () => void;
}) => {
  const { t } = useTranslate();
  return (
    <StyledHeader onClick={onToggle}>
      <StyledTitle>
        {t('export_advanced_settings_title', 'Advanced settings')}
      </StyledTitle>
      {!expanded && (
        <StyledSummary data-cy="export-advanced-settings-summary">
          {summary.map((item, index) => (
            <span
              key={index}
              className={item.state}
              data-cy="export-advanced-settings-summary-item"
              data-cy-state={item.state}
            >
              {item.text}
            </span>
          ))}
        </StyledSummary>
      )}
      <IconButton
        sx={{ ml: 'auto', my: -1 }}
        aria-expanded={expanded}
        aria-label={t('export_advanced_settings_toggle', 'Advanced settings')}
        data-cy="export-advanced-settings-toggle"
      >
        {expanded ? (
          <ChevronUp width={20} height={20} />
        ) : (
          <ChevronDown width={20} height={20} />
        )}
      </IconButton>
    </StyledHeader>
  );
};
