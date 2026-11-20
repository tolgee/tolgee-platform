import { FunctionComponent, useMemo } from 'react';
import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { buildActivity } from 'tg.component/activity/activityTools';
import { ActivityTitle } from 'tg.component/activity/ActivityTitle';
import { ActivityFields } from 'tg.component/activity/ActivityCompact/CompactFields';
import {
  ActivityModel,
  Field,
  ProjectActivityModel,
} from 'tg.component/activity/types';
import { ActivityRowShell } from './ActivityRowShell';
import { ServiceIcon } from './ServiceIcon';

const StyledTitle = styled(Box)`
  min-width: 0;
  overflow: hidden;

  &.lowercase > div > div:first-of-type::first-letter {
    text-transform: lowercase;
  }

  && .reference {
    background: transparent;
    border: none;
    padding: 0px;
    max-height: none;
  }

  && .reference:not(.referenceLink) {
    color: ${({ theme }) => theme.palette.text.primary};
    font-weight: 500;
  }

  && .referenceComposed {
    gap: 5px;
  }

  && > div,
  && .reference,
  && .referenceComposed {
    align-items: baseline;
  }

  && .reference span:has(img) {
    align-self: center;
  }

  && .reference div:has(> img) {
    width: auto !important;
    height: auto !important;
    border-radius: 0px;
    overflow: visible;
    filter: none;
  }

  && .reference img {
    position: static;
    width: auto;
    height: 16px;
  }
`;

type Props = {
  data: ProjectActivityModel;
  diffEnabled: boolean;
  onOpen?: (data: ActivityModel) => void;
  expanded?: boolean;
};

export const ActivityRevisionRow: FunctionComponent<Props> = ({
  data,
  diffEnabled,
  onOpen,
  expanded,
}) => {
  const { t } = useTranslate();
  const activity = useMemo(() => buildActivity(data, true), [data]);
  const description = activity.options?.description?.(data);
  const hasTaskReference = activity.references.some((r) => r.type === 'task');
  const dismissedProvider =
    data.type === 'DISMISS_AUTO_TRANSLATED_STATE'
      ? (Object.values(data.modifiedEntities ?? {})
          .flat()
          .map(
            (entity) =>
              (entity as { modifications?: Record<string, { old?: unknown }> })
                .modifications?.mtProvider?.old
          )
          .find(Boolean) as string | undefined) ?? null
      : undefined;
  const firstField: Field | undefined =
    hasTaskReference || dismissedProvider !== undefined
      ? undefined
      : activity.entities[0]?.fields[0];

  return (
    <>
      <ActivityRowShell
        data-cy="activity-revision-row"
        data-cy-type={data.type}
        author={data.author}
        timestamp={data.timestamp}
        onClick={onOpen && (() => onOpen(data as ActivityModel))}
        chevron={expanded === undefined ? 'right' : expanded ? 'up' : 'down'}
        title={
          <>
            <b>
              {data.author?.name ||
                t('activity_group_unknown_author', 'Unknown user')}
            </b>
            <StyledTitle
              className={activity.translation ? 'lowercase' : undefined}
            >
              <ActivityTitle activity={activity} />
            </StyledTitle>
            {dismissedProvider && <ServiceIcon provider={dismissedProvider} />}
          </>
        }
        detail={
          description ||
          (firstField && (
            <ActivityFields fields={[firstField]} diffEnabled={diffEnabled} />
          ))
        }
      />
    </>
  );
};
