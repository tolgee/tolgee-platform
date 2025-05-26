import { Plus } from '@untitled-ui/icons-react';
import { styled } from '@mui/material';
import { T } from '@tolgee/react';
import clsx from 'clsx';
import React from 'react';

const Wrap = styled('div')`
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 14px;
  min-width: 24px;

  &.with-label {
    padding: 3px 6px;
    & > .control-text {
      padding: 0 4px;
    }
  }
`;

type Props = {
  onClick: (event: React.MouseEvent<HTMLElement>) => void;
  className?: string;
  showText?: boolean;
};

export const AddLabel: React.FC<Props> = ({ onClick, className, showText }) => {
  return (
    <Wrap
      onClick={onClick}
      className={clsx(showText && 'with-label', className)}
      data-cy="translation-label-add"
    >
      <Plus width={16} height={16} />
      {showText && (
        <div className="control-text">
          <T keyName="translations_add_label" />
        </div>
      )}
    </Wrap>
  );
};
