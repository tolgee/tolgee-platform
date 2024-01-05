import { Box, MenuItem, Select, Typography } from '@mui/material';
import { useTolgee, useTranslate } from '@tolgee/react';
import { locales } from '../../../locales';
import { useCurrentLanguage } from 'tg.hooks/useCurrentLanguage';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';

export const LanguageItem = () => {
  const tolgee = useTolgee();
  const language = useCurrentLanguage();

  const { t } = useTranslate();
  return (
    <Box sx={{ padding: '10px 16px 6px 16px' }} display="grid">
      <Typography variant="caption">{t('language_menu_label')}</Typography>
      <Select
        size="small"
        value={language}
        onChange={(e) => {
          tolgee.changeLanguage(e.target.value);
        }}
      >
        {Object.entries(locales).map(([abbr, lang]) => (
          <MenuItem selected={language === abbr} value={abbr} key={abbr}>
            <Box display="flex" gap={0.7} alignItems="center">
              <CircledLanguageIcon
                flag={lang.flag}
                size={18}
                draggable="false"
              />
              {lang.name}
            </Box>
          </MenuItem>
        ))}
      </Select>
    </Box>
  );
};
