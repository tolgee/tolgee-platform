import {
  Box,
  Button,
  ButtonGroup,
  Tooltip,
  Typography,
  buttonGroupClasses,
  styled,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Moon01, Sun } from '@untitled-ui/icons-react';
import { useThemeContext } from '../../../ThemeProvider';

const StyledButtonGroup = styled(ButtonGroup)`
  & .${buttonGroupClasses.grouped} {
    min-width: 70px;
  }
`;

export const ThemeItem = () => {
  const { mode, setMode } = useThemeContext();

  const { t } = useTranslate();
  return (
    <Box
      sx={{ padding: '6px 16px 12px 16px' }}
      display="grid"
      data-cy="user-menu-theme-switch"
    >
      <Typography variant="caption">{t('theme_mode')}</Typography>
      <StyledButtonGroup size="small">
        <Tooltip
          title={t('theme_light_label')}
          enterDelay={1000}
          disableInteractive
        >
          <Button
            color={mode === 'light' ? 'primary' : 'default'}
            onClick={() => setMode('light')}
          >
            <Sun height={18} />
          </Button>
        </Tooltip>
        <Tooltip
          title={t('theme_dark_label')}
          enterDelay={1000}
          disableInteractive
        >
          <Button
            color={mode === 'dark' ? 'primary' : 'default'}
            onClick={() => setMode('dark')}
          >
            <Moon01 height={18} />
          </Button>
        </Tooltip>
        <Button
          color={mode === undefined ? 'primary' : 'default'}
          onClick={() => setMode(undefined)}
        >
          {t('theme_system_label')}
        </Button>
      </StyledButtonGroup>
    </Box>
  );
};
