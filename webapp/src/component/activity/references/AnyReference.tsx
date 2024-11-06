import { Box, styled } from '@mui/material';
import React from 'react';
import { Reference } from '../types';
import { CommentReference } from './CommentReference';
import { KeyReference } from './KeyReference';
import { LanguageReference } from './LanguageReference';
import { ContentDeliveryReference } from './ContentDeliveryReference';
import { ContentStorageReference } from './ContentStorageReference';
import { WebhookConfigReference } from './WebhookConfigReference';
import { TaskReference } from './TaskReference';

const StyledReferences = styled(Box)`
  display: flex;
  gap: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  & .referenceComposed {
    display: flex;
    gap: 3px;
    align-items: center;
    overflow: hidden;
  }

  & .referenceText {
    display: block;
    flex-shrink: 1;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  & .referencePrefix {
    display: inline;
    margin-right: 4px;
    padding-right: 4px;
    border-right: 1px solid
      ${({ theme }) =>
        theme.palette.mode === 'dark'
          ? theme.palette.emphasis[200]
          : theme.palette.emphasis[100]};
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  & .reference {
    background: ${({ theme }) => theme.palette.emphasis[50]};
    max-height: 1.5em;
    padding: 0px 4px;
    border-radius: 4px;
    border: 1px solid
      ${({ theme }) =>
        theme.palette.mode === 'dark'
          ? theme.palette.emphasis[200]
          : theme.palette.emphasis[100]};
  }

  & .reference:not(.referenceLink) {
    color: ${({ theme }) => theme.palette.primaryText};
  }
`;

type Props = {
  data: Reference[];
};

const getReference = (reference: Reference) => {
  switch (reference.type) {
    case 'comment':
      return <CommentReference data={reference} />;
    case 'key':
      return <KeyReference data={reference} />;
    case 'language':
      return <LanguageReference data={reference} />;
    case 'content_delivery_config':
      return <ContentDeliveryReference data={reference} />;
    case 'content_storage':
      return <ContentStorageReference data={reference} />;
    case 'webhook_config':
      return <WebhookConfigReference data={reference} />;
    case 'task':
      return <TaskReference data={reference} />;
    default:
      return null;
  }
};

export const AnyReference: React.FC<Props> = ({ data }) => {
  return (
    <StyledReferences>
      {data.map((ref, i) => {
        return <React.Fragment key={i}>{getReference(ref)}</React.Fragment>;
      })}
    </StyledReferences>
  );
};
