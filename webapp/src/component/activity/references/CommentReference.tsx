import React from 'react';
import { CommentReferenceData } from '../types';

type Props = {
  data: CommentReferenceData;
};

export const CommentReference: React.FC<React.PropsWithChildren<Props>> = ({
  data,
}) => {
  return (
    <span className="reference commmentReference referenceText">
      {data.text}
    </span>
  );
};
