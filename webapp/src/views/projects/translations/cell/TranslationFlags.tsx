import { Box, styled } from '@mui/material';
import { Flag02 } from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { AutoTranslationIcon } from 'tg.component/AutoTranslationIcon';
import { TranslationFlagIcon } from 'tg.component/TranslationFlagIcon';

import { useTranslationsActions } from '../context/TranslationsContext';
import { TranslationTaskIndicator } from 'tg.ee';
import { CloseButton } from 'tg.component/common/buttons/CloseButton';
import React from 'react';

type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

const StyledWrapper = styled('div')`
  display: flex;
`;

const ActiveFlagCircle = styled(Flag02)`
  color: ${({ theme }) => theme.palette.primary.main};
`;

export const StyledTranslationFlagsContainer = styled(Box)`
  display: inline-flex;
  flex-grow: 0;
  align-items: center;
  height: 24px;
  border: 1px solid transparent;
  border-radius: 10px;
  padding: 3px 8px;

  &:hover {
    border: 1px solid ${({ theme }) => theme.palette.tokens.border.secondary};
  }

  &.clickDisabled {
    cursor: default;
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
  const task = keyData.tasks?.find((t) => t.languageTag === lang);

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
        path: { projectId: project.id, translationId: translation!.id! },
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
          translationId: translation!.id!,
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

  return (
    <StyledWrapper className={className}>
      {task && <TranslationTaskIndicator task={task} />}
      {translation?.auto && (
        <CloseButton
          data-cy="translations-auto-translated-clear-button"
          onClose={(e) =>
            handleClearAutoTranslated(
              e as React.MouseEvent<SVGSVGElement, MouseEvent>
            )
          }
          xs
        >
          <StyledTranslationFlagsContainer data-cy="translations-auto-translated-indicator">
            <AutoTranslationIcon provider={translation.mtProvider} />
          </StyledTranslationFlagsContainer>
        </CloseButton>
      )}
      {translation?.outdated && (
        <CloseButton
          data-cy="translations-outdated-clear-button"
          onClose={(e) =>
            handleClearOutdated(
              e as React.MouseEvent<SVGSVGElement, MouseEvent>
            )
          }
          xs
        >
          <StyledTranslationFlagsContainer data-cy="translations-outdated-indicator">
            <TranslationFlagIcon
              tooltip={t('translations_cell_outdated')}
              icon={<ActiveFlagCircle />}
            />
          </StyledTranslationFlagsContainer>
        </CloseButton>
      )}
    </StyledWrapper>
  );
};
