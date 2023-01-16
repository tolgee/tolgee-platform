import { styled, Tooltip } from '@mui/material';
import { Help } from '@mui/icons-material';
import { useTranslate } from '@tolgee/react';
import clsx from 'clsx';
import { components } from 'tg.service/apiSchema.generated';
import { LanguageRow } from './LanguageRow';
import { TABLE_CENTERED, TABLE_DIVIDER, TABLE_TOP_ROW } from '../tableStyles';

const StyledPrimaryProvider = styled('div')`
  display: flex;
  gap: 4px;
  align-items: center;
`;

const StyledHelpIcon = styled(Help)`
  font-size: 15px;
`;

type PagedModelLanguageModel = components['schemas']['PagedModelLanguageModel'];

type Props = {
  providers: string[];
  expanded: boolean;
  languages: PagedModelLanguageModel | undefined;
};

export const SettingsForm = ({ providers, expanded, languages }: Props) => {
  const { t } = useTranslate();

  return (
    <>
      <div className={TABLE_TOP_ROW} />
      {providers.map((provider) => (
        <div key={provider} className={clsx(TABLE_TOP_ROW, TABLE_CENTERED)}>
          {provider}
        </div>
      ))}
      <div className={clsx(TABLE_TOP_ROW, TABLE_CENTERED)}>
        <Tooltip title={t('project_languages_primary_provider_hint')}>
          <StyledPrimaryProvider>
            <div>{t('project_languages_primary_provider', 'Primary')}</div>
            <StyledHelpIcon />
          </StyledPrimaryProvider>
        </Tooltip>
      </div>
      <LanguageRow lang={null} providers={providers} />

      {expanded && (
        <>
          <div className={TABLE_DIVIDER} />
          {languages?._embedded?.languages
            ?.filter(({ base }) => !base)
            .map((lang) => (
              <LanguageRow key={lang.id} lang={lang} providers={providers} />
            ))}
        </>
      )}
    </>
  );
};
