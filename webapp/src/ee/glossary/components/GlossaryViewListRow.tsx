import { Box, Checkbox, styled } from '@mui/material';
import React from 'react';
import { components } from 'tg.service/apiSchema.generated';

type GlossaryTermModel = components['schemas']['GlossaryTermModel'];

const StyledRow = styled('div')`
  display: grid;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-width: 1px 0px 0px 0px;
  position: relative;

  &.deleted {
    text-decoration: line-through;
    pointer-events: none;
  }
`;

const StyledRowCell = styled('div')`
  display: flex;
  flex-grow: 0;
  align-content: center;
  align-items: center;
`;

type Props = {
  item: GlossaryTermModel;
  selectedLanguages: string[] | undefined;
  selectedTerms: number[];
  selectedTermsInverted: boolean;
  onToggleSelectedTerm: (term: number) => void;
};

export const GlossaryViewListRow: React.VFC<Props> = ({
  item,
  selectedLanguages,
  selectedTerms,
  selectedTermsInverted,
  onToggleSelectedTerm,
}) => {
  const checked = selectedTerms.includes(item.id) != selectedTermsInverted;

  return (
    <StyledRow
      key={item.id}
      style={{
        gridTemplateColumns:
          'minmax(350px, 1fr)' +
          ' minmax(350px, 1fr)'.repeat(selectedLanguages?.length || 0),
      }}
    >
      <StyledRowCell>
        <Checkbox
          checked={checked}
          onChange={() => {
            onToggleSelectedTerm(item.id);
          }}
        />
        <Box>{item.description}</Box>
      </StyledRowCell>
      {selectedLanguages?.map((tag, i) => {
        return (
          <StyledRowCell key={i + 1}>
            <Box>{tag}</Box>
          </StyledRowCell>
        );
      })}
    </StyledRow>
  );
};
