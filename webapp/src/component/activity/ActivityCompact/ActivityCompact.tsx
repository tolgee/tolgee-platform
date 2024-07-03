import { useMemo } from 'react';
import { DotsVertical } from '@untitled-ui/icons-react';
import { Box, IconButton, styled } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { buildActivity } from '../activityTools';
import { ActivityTitle } from '../ActivityTitle';
import { ActivityFields } from './CompactFields';
import { ActivityModel, Field } from '../types';
import { ActivityUser } from '../ActivityUser';
import { actionsConfiguration } from '../configuration';

type ProjectActivityModel = components['schemas']['ProjectActivityModel'];

const MAX_ENTITIES = 1;
const MAX_FIELDS = 1;

const StyledContainer = styled('div')`
  display: contents;
  & > * {
    padding: 4px 8px;
    transition: background 0.1s ease-out;
  }

  &:hover > * {
    transition: background 0.3s ease-in;
    background: ${({ theme }) => theme.palette.cell.hover};
  }

  & > * {
    overflow: hidden;
  }

  & .showOnMouseOver {
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.1s ease-out;
  }

  &:focus-within .showOnMouseOver {
    opacity: 1;
  }

  &:hover .showOnMouseOver {
    opacity: 1;
    pointer-events: all;
    transition: opacity 0.3s ease-in;
  }
`;

const StyledUser = styled(Box)`
  border-radius: 8px 0px 0px 8px;
  margin-left: 4px;
`;

const StyledContent = styled(Box)`
  display: grid;
`;

const StyledAction = styled(Box)`
  padding: 0px;
  margin-right: 4px;
  border-radius: 0px 8px 8px 0px;
`;

const StyledMoreIndicator = styled(Box)`
  margin-top: -10px;
  font-size: 14px;
  cursor: pointer;
  justify-self: start;
`;

type Props = {
  data: ProjectActivityModel;
  diffEnabled: boolean;
  onDetailOpen: (data: ActivityModel) => void;
};

export const ActivityCompact = ({ data, diffEnabled, onDetailOpen }: Props) => {
  const activity = useMemo(() => buildActivity(data, true), [data]);

  let fieldsNum = 0;

  activity.entities.forEach((e) => {
    fieldsNum += e.fields.length;
  });

  const limitedFields: Field[] = [];

  const maxFields =
    actionsConfiguration[data.type]?.compactFieldCount || MAX_FIELDS;

  activity.entities.slice(0, MAX_ENTITIES).forEach((e) => {
    e.fields.slice(0, maxFields).forEach((f) => {
      limitedFields.push(f);
    });
  });

  return (
    <StyledContainer data-cy="activity-compact">
      <StyledUser>
        <ActivityUser item={data} onlyTime />
      </StyledUser>
      <StyledContent>
        <ActivityTitle activity={activity} />
        <ActivityFields fields={limitedFields} diffEnabled={diffEnabled} />
        {fieldsNum > maxFields && (
          <StyledMoreIndicator onClick={() => onDetailOpen(data)}>
            ...
          </StyledMoreIndicator>
        )}
        {/* <pre>{JSON.stringify(data, null, 2)}</pre> */}
      </StyledContent>
      <StyledAction>
        <IconButton
          data-cy="activity-compact-detail-button"
          className="showOnMouseOver"
          size="small"
          onClick={() => onDetailOpen(data)}
        >
          <DotsVertical />
        </IconButton>
      </StyledAction>
    </StyledContainer>
  );
};
