import React from 'react';
import { Box, styled } from '@mui/material';

import { Activity } from '../types';
import { formatDiff } from '../formatTools';
import { EntityDescription } from '../EntityDescriptioin';

const StyledFields = styled('div')`
  display: grid;
  grid-template-columns: auto 1fr;
`;

const StyledEntityTitle = styled('div')`
  font-size: 16px;
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
`;

const StyledFieldLabel = styled(Box)`
  text-align: right;
  margin-left: 10px;
`;

const StyledFieldContent = styled(Box)`
  margin-left: 10px;
  word-break: break-all;
`;

const StyledSeparator = styled(Box)`
  height: 1px;
  background: ${({ theme }) => theme.palette.divider};
  margin: 10px 0px;
`;

type Props = {
  activity: Activity;
  diffEnabled: boolean;
  showAllReferences?: boolean;
};

export const ActivityEntities: React.FC<Props> = ({
  activity,
  diffEnabled,
  showAllReferences,
}) => {
  return (
    <StyledFields>
      {activity.entities.map((entity, i) => {
        if (!entity?.fields.length) {
          return null;
        }
        return (
          <React.Fragment key={i}>
            <StyledSeparator gridColumn="1 / span 2" />
            <StyledEntityTitle sx={{ gridColumn: '1 / span 2' }}>
              {entity.options.label?.()}
              <EntityDescription
                entity={entity}
                showAllReferences={showAllReferences}
              />
            </StyledEntityTitle>
            {entity.fields.map((field, i) => {
              const label = field.options.label;
              const value = formatDiff({
                value: field.value,
                options: field.options,
                diffEnabled,
                languageTag: field.languageTag,
              });
              return value ? (
                <React.Fragment key={i}>
                  {label && <StyledFieldLabel>{label?.()}:</StyledFieldLabel>}
                  <StyledFieldContent
                    gridColumn={!label ? '1 / span 2' : undefined}
                  >
                    {value}
                  </StyledFieldContent>
                </React.Fragment>
              ) : null;
            })}
          </React.Fragment>
        );
      })}
    </StyledFields>
  );
};
