import React from 'react';
import { styled } from '@mui/material';
import { TruncatedTooltip } from 'tg.component/activity/TruncatedTooltip';

import { formatDiff } from '../formatTools';
import { Field } from '../types';

const StyledFields = styled('div')`
  display: grid;
`;

type Props = {
  fields: Field[];
  diffEnabled: boolean;
};

export const ActivityFields: React.FC<React.PropsWithChildren<Props>> = ({
  fields,
  diffEnabled,
}) => {
  return (
    <StyledFields>
      {fields.map((field, i) => {
        const content = formatDiff({
          value: field.value,
          options: field.options,
          diffEnabled,
          languageTag: field.languageTag,
        });
        return (
          <TruncatedTooltip key={i} tooltip={content}>
            {content}
          </TruncatedTooltip>
        );
      })}
    </StyledFields>
  );
};
