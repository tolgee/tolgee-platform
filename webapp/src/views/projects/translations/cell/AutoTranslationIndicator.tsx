import { styled } from '@mui/material';
import { Clear } from '@mui/icons-material';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useTranslationsDispatch } from '../context/TranslationsContext';
import { AutoTranslationIcon } from '../../../../component/AutoTranslationIcon';

type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

const StyledWrapper = styled('div')`
  height: 0px;
`;

const StyledClearButton = styled(Clear)`
  padding-left: 2px;
  font-size: 18px;
  display: none;
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

  &:hover ${StyledClearButton} {
    display: block;
  }

  &:hover {
    border: 1px solid ${({ theme }) => theme.palette.divider2.main};
    transition: all 0.1s;
  }
`;

type Props = {
  keyData: KeyWithTranslationsModel;
  lang: string;
  className?: string;
};

export const AutoTranslationIndicator: React.FC<Props> = ({
  keyData,
  lang,
  className,
}) => {
  const project = useProject();
  const translation = keyData.translations[lang];

  const dispatch = useTranslationsDispatch();

  const clear = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/dismiss-auto-translated-state',
    method: 'put',
  });

  const handleClear = (e: React.MouseEvent<SVGSVGElement, MouseEvent>) => {
    e.stopPropagation();
    clear
      .mutateAsync({
        path: { projectId: project.id, translationId: translation!.id },
      })
      .then(() => {
        dispatch({
          type: 'UPDATE_TRANSLATION',
          payload: { keyId: keyData.keyId, lang, data: { auto: false } },
        });
      });
  };

  if (translation?.auto) {
    return (
      <StyledWrapper className={className}>
        <StyledContainer data-cy="translations-auto-translated-indicator">
          <AutoTranslationIcon provider={translation.mtProvider} />
          <StyledClearButton
            role="button"
            onClick={handleClear}
            data-cy="translations-auto-translated-clear-button"
          />
        </StyledContainer>
      </StyledWrapper>
    );
  } else {
    return null;
  }
};
