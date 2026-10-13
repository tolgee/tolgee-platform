import { useEffect, useState } from 'react';
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
import {
  ExportFormValues,
  hasCustomAdvancedSettings,
} from 'tg.views/projects/export/exportSettings';

const StyledHeader = styled('div')`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.5)};
  justify-self: start;
  min-height: 40px;

  &.toggleable {
    cursor: pointer;
  }
`;

const StyledTitle = styled('div')`
  font-size: ${({ theme }) => theme.typography.subtitle2.fontSize}px;
  font-weight: ${({ theme }) => theme.typography.subtitle2.fontWeight};
  color: ${({ theme }) => theme.palette.text.primary};
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
  onExpandedChange?: (expanded: boolean) => void;
};

export const ExportAdvancedSettings = ({
  allNamespaces,
  collapsible = false,
  expanded = true,
  onExpandedChange,
}: Props) => {
  const { t } = useTranslate();
  const { values } = useFormikContext<ExportFormValues>();
  const lockedOpen =
    collapsible && hasCustomAdvancedSettings(values, allNamespaces ?? []);
  const [keptOpen, setKeptOpen] = useState(false);

  useEffect(() => {
    if (lockedOpen) {
      setKeptOpen(true);
    }
  }, [lockedOpen]);

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

  const isOpen = expanded || lockedOpen || keptOpen;
  const toggle = () => {
    setKeptOpen(false);
    onExpandedChange?.(!isOpen);
  };

  return (
    <Box
      display="grid"
      gridTemplateColumns="minmax(0, 1fr)"
      gap={1}
      data-cy="export-advanced-settings"
    >
      <StyledHeader
        className={lockedOpen ? undefined : 'toggleable'}
        onClick={lockedOpen ? undefined : toggle}
      >
        <StyledTitle>
          {t('export_advanced_settings_title', 'Advanced settings')}
        </StyledTitle>
        {!lockedOpen && (
          <IconButton
            size="small"
            sx={{ my: -1 }}
            aria-expanded={isOpen}
            aria-label={t(
              'export_advanced_settings_toggle',
              'Advanced settings'
            )}
            data-cy="export-advanced-settings-toggle"
          >
            {isOpen ? (
              <ChevronUp width={20} height={20} />
            ) : (
              <ChevronDown width={20} height={20} />
            )}
          </IconButton>
        )}
      </StyledHeader>
      {isOpen && content}
    </Box>
  );
};
