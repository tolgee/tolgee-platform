import { useEffect } from 'react';
import { useTranslate } from '@tolgee/react';
import { styled } from '@mui/material';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { stringHash } from 'tg.fixtures/stringHash';
import { LoadingSkeletonFadingIn } from 'tg.component/LoadingSkeleton';

import { TabMessage } from '../../common/TabMessage';
import { PanelContentProps } from '../../common/types';
import { TranslationMemoryItem } from './TranslationMemoryItem';

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
`;

export const TranslationMemory: React.FC<PanelContentProps> = ({
  keyData,
  language,
  baseLanguage,
  project,
  setValue,
  setItemsCount,
  activeVariant,
}) => {
  const { t } = useTranslate();

  const deps = {
    keyId: keyData.keyId,
    targetLanguageId: language.id,
    isPlural: keyData.keyIsPlural,
  };

  const dependenciesHash = stringHash(JSON.stringify(deps));

  const memory = useApiQuery({
    url: '/v2/projects/{projectId}/suggest/translation-memory',
    method: 'post',
    // @ts-ignore add all dependencies to properly update query
    query: { hash: dependenciesHash, size: 2 },
    path: { projectId: project.id },
    content: {
      'application/json': deps,
    },
  });

  const data = memory.data;
  const items = data?._embedded?.translationMemoryItems;

  useEffect(() => {
    setItemsCount(items?.length ?? 0);
  }, [items?.length]);

  if (memory.isLoading) {
    return (
      <StyledContainer>
        <TabMessage>
          <LoadingSkeletonFadingIn variant="text" />
        </TabMessage>
      </StyledContainer>
    );
  }

  return (
    <StyledContainer>
      {items?.length ? (
        items.map((item, i) => (
          <TranslationMemoryItem
            key={i}
            item={item}
            setValue={setValue}
            languageTag={language.tag}
            baseLanguageTag={baseLanguage.tag}
            pluralVariant={activeVariant}
          />
        ))
      ) : (
        <TabMessage>
          {t('translation_tools_nothing_found', 'Nothing found')}
        </TabMessage>
      )}
    </StyledContainer>
  );
};
