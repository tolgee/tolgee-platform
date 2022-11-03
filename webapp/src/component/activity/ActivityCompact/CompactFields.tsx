import React from 'react';
import { Box, styled } from '@mui/material';

import { formatDiff } from '../formatTools';
import { Field } from '../types';

const StyledFields = styled('div')`
  display: grid;
`;

const StyledField = styled(Box)`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

type Props = {
  fields: Field[];
  diffEnabled: boolean;
};

export const ActivityFields: React.FC<Props> = ({ fields, diffEnabled }) => {
  return (
    <StyledFields>
      {fields.map((field, i) => {
        return (
          <StyledField key={i}>
            {formatDiff({
              value: field.value,
              options: field.options,
              diffEnabled,
              languageTag: field.languageTag,
            })}
          </StyledField>
        );
      })}
    </StyledFields>
  );
};
