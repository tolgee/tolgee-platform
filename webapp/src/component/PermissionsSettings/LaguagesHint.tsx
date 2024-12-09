import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { LanguageModel, PermissionModel } from './types';
import { LanguageScope } from './hierarchyTools';

const StyledHint = styled('div')`
  display: grid;
  gap: 8px;
`;

const StyledHintLabel = styled('div')`
  font-weight: bold;
`;

const StyledHintData = styled('div')`
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
`;

const StyledLanguage = styled('div')`
  display: flex;
  gap: 2px;
`;

export type LanguagePermissionCategory = {
  label: string;
  data: number[] | undefined;
  scope: LanguageScope;
};

type Props = {
  categories: LanguagePermissionCategory[];
  permissions: PermissionModel;
  allLangs: LanguageModel[];
};

export function LanguagesHint({ categories, permissions, allLangs }: Props) {
  const { t } = useTranslate();

  function getLanguage(id: number) {
    return allLangs.find((l) => l.id === id);
  }

  return (
    <StyledHint>
      {categories
        .filter(({ scope }) => permissions.scopes.includes(scope))
        .map(({ label, data }, i) => (
          <div key={i}>
            <StyledHintLabel>{label}</StyledHintLabel>
            <StyledHintData>
              {data?.length
                ? data?.map(getLanguage).map((language, i) => (
                    <StyledLanguage key={i}>
                      <div>{language?.name || language?.tag}</div>
                      <div>
                        <CircledLanguageIcon
                          size={12}
                          flag={language?.flagEmoji}
                        />
                      </div>
                    </StyledLanguage>
                  ))
                : t('languages_permitted_list_all')}
            </StyledHintData>
          </div>
        ))}
    </StyledHint>
  );
}
