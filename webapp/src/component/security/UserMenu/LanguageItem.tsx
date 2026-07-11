import {
  Box,
  MenuItem,
  Select,
  Typography,
  selectClasses,
  styled,
} from '@mui/material';
import { useTolgee, useTranslate } from '@tolgee/react';
import { locales } from '@tginternal/library/constants/locales';
import { useCurrentLanguage } from '@tginternal/library/hooks/useCurrentLanguage';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';

const StyledSelect = styled(Select)`
  & .${selectClasses.select} {
    padding: 6px 14px;
  }
`;

export const LanguageItem = () => {
  const tolgee = useTolgee();
  const language = useCurrentLanguage();

  const { t } = useTranslate();
  return (
    <Box
      sx={{ padding: '10px 16px 6px 16px' }}
      display="grid"
      data-cy="user-menu-language-switch"
    >
      <Typography variant="caption">{t('language_menu_label')}</Typography>
      <StyledSelect
        size="small"
        value={language}
        onChange={(e) => {
          tolgee.changeLanguage(e.target.value as any);
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
      </StyledSelect>
    </Box>
  );
};
