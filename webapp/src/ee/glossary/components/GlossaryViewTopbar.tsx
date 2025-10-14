import { Box, IconButton, styled, Tooltip } from '@mui/material';
import { SecondaryBarSearchField } from 'tg.component/layout/SecondaryBarSearchField';
import { GlossaryViewLanguageSelect } from 'tg.ee.module/glossary/components/GlossaryViewLanguageSelect';
import { BaseViewAddButton } from 'tg.component/layout/BaseViewAddButton';
import React from 'react';
import { useTranslate } from '@tolgee/react';
import { FileDownload03, UploadCloud02 } from '@untitled-ui/icons-react';
import { useGlossaryExport } from 'tg.ee.module/glossary/hooks/useGlossaryExport';

const StyledContainerInner = styled(Box)`
  display: grid;
  width: 100%;
  margin: 0px auto;
  margin-top: 0px;
  margin-bottom: 0px;
`;

type Props = {
  onCreateTerm?: () => void;
  onImport?: () => void;
  onSearch?: (search: string) => void;
  search?: string;
  selectedLanguages?: string[];
  setSelectedLanguages: (languages: string[]) => void;
  clearSearchCallbackRef?: React.MutableRefObject<(() => void) | null>;
};

export const GlossaryViewTopbar = ({
  onCreateTerm,
  onImport,
  onSearch,
  search,
  selectedLanguages,
  setSelectedLanguages,
  clearSearchCallbackRef,
}: Props) => {
  const { t } = useTranslate();
  const { triggerExport, exportLoading } = useGlossaryExport();

  return (
    <Box>
      <StyledContainerInner>
        <Box display="flex" justifyContent="space-between">
          <Box display="flex" alignItems="center" gap="8px">
            <Box>
              <SecondaryBarSearchField
                onSearch={onSearch}
                initial={search}
                placeholder={t('glossary_search_placeholder')}
                clearCallbackRef={clearSearchCallbackRef}
              />
            </Box>
          </Box>
          <Box display="flex" gap={1}>
            <GlossaryViewLanguageSelect
              value={selectedLanguages}
              onValueChange={setSelectedLanguages}
              sx={{
                width: '250px',
              }}
            />
            {onImport && (
              <Tooltip title={t('glossary_import_button')}>
                <IconButton
                  size="small"
                  color="primary"
                  onClick={onImport}
                  data-cy="glossary-import-button"
                  aria-label={t('glossary_import_button')}
                >
                  <UploadCloud02 height={20} width={20} />
                </IconButton>
              </Tooltip>
            )}
            <Tooltip title={t('glossary_export_button')}>
              <IconButton
                size="small"
                color="primary"
                onClick={triggerExport}
                data-cy="glossary-export-button"
                disabled={exportLoading}
                aria-label={t('glossary_export_button')}
              >
                <FileDownload03 height={20} width={20} />
              </IconButton>
            </Tooltip>
            {onCreateTerm && (
              <BaseViewAddButton
                onClick={onCreateTerm}
                label={t('glossary_add_button')}
              />
            )}
          </Box>
        </Box>
      </StyledContainerInner>
    </Box>
  );
};
