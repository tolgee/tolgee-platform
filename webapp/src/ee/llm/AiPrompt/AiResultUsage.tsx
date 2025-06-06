import { Box, styled, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type PromptResponseUsageDto = components['schemas']['PromptResponseUsageDto'];

const StyledContainer = styled(Box)`
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const SpanUnderlined = styled('span')`
  text-decoration: underline;
  text-decoration-style: dashed;
  text-underline-offset: 3px;
`;

type Props = {
  usage: PromptResponseUsageDto | undefined;
  price: number | undefined;
};

export const AiResultUsage = ({ usage, price }: Props) => {
  const { t } = useTranslate();
  return (
    <StyledContainer minHeight={20}>
      {Boolean(usage?.inputTokens || usage?.outputTokens) && (
        <span>
          <Tooltip title={t('ai_playground_usage_tokens_hint')}>
            <SpanUnderlined>{t('ai_playground_usage_tokens')}</SpanUnderlined>
          </Tooltip>
          :{' '}
          <span data-cy="ai-playground-usage-tokens">
            {Number(usage?.inputTokens ?? 0) + Number(usage?.outputTokens ?? 0)}
          </span>
        </span>
      )}

      {Boolean(usage?.cachedTokens) && (
        <span>
          <Tooltip title={t('ai_playground_usage_cached_tokens_hint')}>
            <SpanUnderlined>
              {t('ai_playground_usage_cached_tokens')}
            </SpanUnderlined>
          </Tooltip>
          :{' '}
          <span data-cy="ai-playground-usage-cached-tokens">
            {usage?.cachedTokens}
          </span>
        </span>
      )}

      {typeof price === 'number' && (
        <span>
          <Tooltip title={t('ai_playground_mt_credits_hint')}>
            <SpanUnderlined>{t('ai_playground_mt_credits')}</SpanUnderlined>
          </Tooltip>
          : <span data-cy="ai-playground-usage-mt-credits">{price / 100}</span>
        </span>
      )}
    </StyledContainer>
  );
};
