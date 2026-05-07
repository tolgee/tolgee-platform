import React from 'react';
import { Button, styled, TextField } from '@mui/material';
import { T } from '@tolgee/react';
import LoadingButton from 'tg.component/common/form/LoadingButton';

const StyledControls = styled('div')`
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
`;

type Props = {
  value: string;
  onChange: (next: string) => void;
  onSave: () => void;
  onCancel: () => void;
  saving: boolean;
  minRows?: number;
  fieldDataCy: string;
  cancelDataCy: string;
  saveDataCy: string;
  cancelLabel?: React.ReactNode;
  saveLabel?: React.ReactNode;
};

export const EditableTextCellForm: React.VFC<Props> = ({
  value,
  onChange,
  onSave,
  onCancel,
  saving,
  minRows = 2,
  fieldDataCy,
  cancelDataCy,
  saveDataCy,
  cancelLabel,
  saveLabel,
}) => {
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      onSave();
      return;
    }
    if (e.key === 'Escape') {
      onCancel();
    }
  };

  return (
    <>
      <TextField
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={handleKeyDown}
        multiline
        minRows={minRows}
        fullWidth
        autoFocus
        size="small"
        data-cy={fieldDataCy}
      />
      <StyledControls>
        <Button
          onClick={onCancel}
          size="small"
          variant="outlined"
          data-cy={cancelDataCy}
        >
          {cancelLabel ?? <T keyName="global_cancel_button" />}
        </Button>
        <LoadingButton
          onClick={onSave}
          size="small"
          variant="contained"
          color="primary"
          loading={saving}
          data-cy={saveDataCy}
        >
          {saveLabel ?? <T keyName="global_form_save" />}
        </LoadingButton>
      </StyledControls>
    </>
  );
};
