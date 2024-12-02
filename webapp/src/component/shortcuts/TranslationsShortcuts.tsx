import { Box, Typography, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { IS_MAC, getMetaName } from 'tg.fixtures/isMac';
import { formatShortcut } from 'tg.fixtures/shortcuts';
import { Shortcut } from './Shortcut';

const StyledContainer = styled(Box)`
  height: 100%;
  padding: 16px 12px 8px 12px;
  display: grid;
  position: relative;
  background: ${({ theme }) => theme.palette.cell.hover};
  border-radius: 16px;
`;

const StyledItems = styled(Box)`
  display: grid;
  gap: 8px;
  padding-top: 8px;
  padding-bottom: 24px;
`;

export const TranslationsShortcuts = () => {
  const listShorttcuts = [
    {
      name: <T keyName="translations_shortcuts_move" />,
      formula: ['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].map((i) =>
        formatShortcut(i)
      ),
    },
    {
      name: <T keyName="translations_cell_edit" />,
      formula: formatShortcut('Enter'),
    },
  ];

  const batchShortcuts = [
    {
      name: <T keyName="translations_shortcut_shift_checkbox" />,
      formula: (
        <Box display="flex" alignItems="center">
          {formatShortcut('Shift')}
          <Box px="2px">+</Box>
          <T keyName="translations_shortcut_click" />
        </Box>
      ),
    },
  ];

  const editorShortcuts = [
    {
      name: <T keyName="translations_cell_save" />,
      formula: formatShortcut('Enter'),
    },
    {
      name: <T keyName="translations_cell_save_and_continue" />,
      formula: formatShortcut(`${getMetaName()} + Enter`),
    },
    {
      name: <T keyName="translations_cell_change_state" />,
      formula: formatShortcut(`${getMetaName()} + E`),
    },
    {
      name: <T keyName="translations_cell_insert_base" />,
      formula: IS_MAC
        ? formatShortcut(`${getMetaName()} + Shift + S`)
        : formatShortcut(`${getMetaName()} + Insert`),
    },
  ];

  return (
    <StyledContainer>
      <Typography variant="subtitle2">
        <T keyName="translations_shortcuts_in_list_title" />
      </Typography>
      <StyledItems>
        {listShorttcuts.map((item, i) => {
          return <Shortcut key={i} {...item} />;
        })}
      </StyledItems>

      <Typography variant="subtitle2">
        <T keyName="translations_shortcuts_in_editor_title" />
      </Typography>
      <StyledItems>
        {editorShortcuts.map((item, i) => {
          return <Shortcut key={i} {...item} />;
        })}
      </StyledItems>

      <Typography variant="subtitle2">
        <T keyName="translations_shortcuts_batch_title" />
      </Typography>
      <StyledItems>
        {batchShortcuts.map((item, i) => {
          return <Shortcut key={i} {...item} />;
        })}
      </StyledItems>
    </StyledContainer>
  );
};
