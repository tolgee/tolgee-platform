import { Box, styled } from '@mui/material';
import { SecondaryBarSearchField } from 'tg.component/layout/SecondaryBarSearchField';
import { GlossaryViewLanguageSelect } from 'tg.ee.module/glossary/components/GlossaryViewLanguageSelect';
import { BaseViewAddButton } from 'tg.component/layout/BaseViewAddButton';
import React from 'react';
import { useTranslate } from '@tolgee/react';

const StyledContainerInner = styled(Box)`
  display: grid;
  width: 100%;
  margin: 0px auto;
  margin-top: 0px;
  margin-bottom: 0px;
`;

type Props = {
  onCreate?: () => void;
  onSearch?: (search: string) => void;
  search?: string;
  selectedLanguages?: string[];
  setSelectedLanguages: (languages: string[]) => void;
  clearSearchCallbackRef?: React.MutableRefObject<(() => void) | null>;
};

export const GlossaryViewTopbar = ({
  onCreate,
  onSearch,
  search,
  selectedLanguages,
  setSelectedLanguages,
  clearSearchCallbackRef,
}: Props) => {
  const { t } = useTranslate();

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
          <Box display="flex" gap={2}>
            <GlossaryViewLanguageSelect
              value={selectedLanguages}
              onValueChange={setSelectedLanguages}
              sx={{
                width: '250px',
              }}
            />
            {onCreate && (
              <BaseViewAddButton
                onClick={onCreate}
                label={t('glossary_add_button')}
              />
            )}
          </Box>
        </Box>
      </StyledContainerInner>
    </Box>
  );
};
