import { useTranslate } from '@tolgee/react';
import { XClose } from '@untitled-ui/icons-react';
import { IconButton, styled, SxProps, Box } from '@mui/material';
import { useState, useRef } from 'react';

import { stopBubble } from 'tg.fixtures/eventHandler';
import { FiltersType, isFilterEmpty, LanguageModel } from './tools';
import { TextField } from 'tg.component/common/TextField';
import { FakeInput } from 'tg.component/FakeInput';
import { ArrowDropDown } from 'tg.component/CustomIcons';
import { TranslationFiltersPopup } from './TranslationFiltersPopup';
import { type FilterActions } from 'tg.views/projects/translations/context/services/useTranslationFilterService';

const StyledInputButton = styled(IconButton)`
  margin: ${({ theme }) => theme.spacing(-1, -0.5, -1, -0.25)};
`;

type Props = {
  actions: FilterActions;
  value: FiltersType;
  selectedLanguages: LanguageModel[];
  placeholder?: React.ReactNode;
  projectId: number;
  sx?: SxProps;
  className?: string;
};

export const TranslationFilters = ({
  value,
  actions,
  selectedLanguages,
  placeholder,
  projectId,
  sx,
  className,
}: Props) => {
  const anchorEl = useRef(null);
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);

  function handleClick() {
    setOpen(true);
  }

  return (
    <>
      <TextField
        variant="outlined"
        // value={getFilterValue(value)}
        data-cy="tasks-header-filter-select"
        minHeight={false}
        placeholder={t('task_filter_placeholder')}
        InputProps={{
          onClick: handleClick,
          ref: anchorEl,
          fullWidth: true,
          sx: {
            cursor: 'pointer',
            minWidth: 200,
          },
          readOnly: true,
          inputComponent: FakeInput,
          margin: 'dense',
          endAdornment: (
            <Box
              sx={{ display: 'flex', marginRight: -0.5, alignItems: 'center' }}
            >
              {!isFilterEmpty(value) && (
                <StyledInputButton
                  size="small"
                  onClick={stopBubble(() => actions.setFilters({}))}
                  tabIndex={-1}
                >
                  <XClose width={20} height={20} />
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
        <TranslationFiltersPopup
          onClose={() => setOpen(false)}
          value={value}
          actions={actions}
          anchorEl={anchorEl.current!}
          projectId={projectId}
          selectedLanguages={selectedLanguages}
        />
      )}
    </>
  );
};
