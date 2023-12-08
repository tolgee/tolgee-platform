import { styled } from '@mui/material';
import { Clear, FlagCircle } from '@mui/icons-material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { AutoTranslationIcon } from 'tg.component/AutoTranslationIcon';
import { TranslationFlagIcon } from 'tg.component/TranslationFlagIcon';
import { useTranslationsActions } from '../context/TranslationsContext';

type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

const StyledWrapper = styled('div')`
  height: 0px;
  display: flex;
  gap: 2px;
`;

const StyledClearButton = styled(Clear)`
  padding-left: 2px;
  font-size: 18px;
  display: none;
`;

const ActiveFlagCircle = styled(FlagCircle)`
  color: ${({ theme }) => theme.palette.primary.main};
`;

const StyledContainer = styled('div')`
  display: inline-flex;
  flex-grow: 0;
  align-items: center;
  height: 20px;
  border: 1px solid transparent;
  padding: 0px 4px;
  margin-left: -4px;
  border-radius: 10px;

  &:hover .clearButton {
    display: block;
  }
  &:hover {
    border: 1px solid ${({ theme }) => theme.palette.divider1};
    transition: all 0.1s;
  }
`;

type Props = {
  keyData: KeyWithTranslationsModel;
  lang: string;
  className?: string;
};

export const TranslationFlags: React.FC<Props> = ({
  keyData,
  lang,
  className,
}) => {
  const project = useProject();
  const { t } = useTranslate();
  const translation = keyData.translations[lang];

  const { updateTranslation } = useTranslationsActions();

  const clearAutoTranslatedState = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/dismiss-auto-translated-state',
    method: 'put',
  });

  const handleClearAutoTranslated = (
    e: React.MouseEvent<SVGSVGElement, MouseEvent>
  ) => {
    e.stopPropagation();
    clearAutoTranslatedState
      .mutateAsync({
        path: { projectId: project.id, translationId: translation!.id },
      })
      .then(() => {
        updateTranslation({
          keyId: keyData.keyId,
          lang,
          data: { auto: false },
        });
      });
  };

  const clearOutdated = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/set-outdated-flag/{state}',
    method: 'put',
  });

  const handleClearOutdated = (
    e: React.MouseEvent<SVGSVGElement, MouseEvent>
  ) => {
    e.stopPropagation();
    clearOutdated
      .mutateAsync({
        path: {
          projectId: project.id,
          translationId: translation!.id,
          state: false,
        },
      })
      .then(() => {
        updateTranslation({
          keyId: keyData.keyId,
          lang,
          data: { outdated: false },
        });
      });
  };

  if (translation?.auto || translation?.outdated) {
    return (
      <StyledWrapper className={className}>
        {translation.auto && (
          <StyledContainer data-cy="translations-auto-translated-indicator">
            <AutoTranslationIcon provider={translation.mtProvider} />
            <StyledClearButton
              role="button"
              onClick={handleClearAutoTranslated}
              data-cy="translations-auto-translated-clear-button"
              className="clearButton"
            />
          </StyledContainer>
        )}
        {translation.outdated && (
          <StyledContainer data-cy="translations-outdated-indicator">
            <TranslationFlagIcon
              tooltip={t('translations_cell_outdated')}
              icon={<ActiveFlagCircle />}
            />
            <StyledClearButton
              role="button"
              onClick={handleClearOutdated}
              data-cy="translations-outdated-clear-button"
            />
          </StyledContainer>
        )}
      </StyledWrapper>
    );
  } else {
    return null;
  }
};
