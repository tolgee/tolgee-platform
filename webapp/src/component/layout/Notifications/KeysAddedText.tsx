import { FunctionComponent } from 'react';
import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type NotificationModel = components['schemas']['NotificationModel'];
type KeySample = components['schemas']['NotificationKeySampleModel'];

const StyledSample = styled(Box)`
  display: flex;
  gap: 6px;
  min-width: 0;
`;

const StyledKeyName = styled(Box)`
  flex-shrink: 0;
  font-family: monospace;
  font-size: 13px;
`;

const StyledBaseText = styled(Box)`
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const fullKeyName = (key: KeySample) =>
  key.namespace ? `${key.namespace}:${key.name}` : key.name;

type Props = {
  notification: NotificationModel;
  entityCap: number;
};

export const KeysAddedText: FunctionComponent<Props> = ({
  notification,
  entityCap,
}) => {
  const { t } = useTranslate();
  const count = notification.entityCount ?? 0;
  const samples = notification.sampleKeys ?? [];
  const project = notification.project?.name ?? '';
  const capped = count >= entityCap;

  if (!capped && count > 0 && count <= 2 && samples.length === count) {
    return (
      <Box data-cy="notifications-keys-added-text">
        <b>
          {t(
            'notifications-keys-added-few',
            '{count, plural, one {New key} other {# new keys}}',
            { count }
          )}
        </b>
        {samples.map((key) => (
          <StyledSample
            key={key.keyId}
            data-cy="notifications-keys-added-sample"
          >
            <StyledKeyName>{fullKeyName(key)}</StyledKeyName>
            {key.baseText && <StyledBaseText>{key.baseText}</StyledBaseText>}
          </StyledSample>
        ))}
      </Box>
    );
  }

  if (samples.length >= 2 && capped) {
    return (
      <Box data-cy="notifications-keys-added-text">
        <b>
          {t(
            'notifications-keys-added-capped',
            '100+ new keys in {project}, e.g. {first}, {second} and more',
            {
              project,
              first: fullKeyName(samples[0]),
              second: fullKeyName(samples[1]),
            }
          )}
        </b>
      </Box>
    );
  }

  if (samples.length >= 2) {
    return (
      <Box data-cy="notifications-keys-added-text">
        <b>
          {t(
            'notifications-keys-added-many',
            '{count} new keys in {project}, e.g. {first}, {second} and {rest} more',
            {
              count,
              project,
              first: fullKeyName(samples[0]),
              second: fullKeyName(samples[1]),
              rest: count - 2,
            }
          )}
        </b>
      </Box>
    );
  }

  if (capped) {
    return (
      <Box data-cy="notifications-keys-added-text">
        <b>
          {t(
            'notifications-keys-added-capped-count',
            '100+ new keys in {project}',
            {
              project,
            }
          )}
        </b>
      </Box>
    );
  }

  return (
    <Box data-cy="notifications-keys-added-text">
      <b>
        {t(
          'notifications-keys-added-count',
          '{count, plural, one {# new key} other {# new keys}} in {project}',
          { count, project }
        )}
      </b>
    </Box>
  );
};
