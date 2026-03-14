import { FieldArray, useFormikContext } from 'formik';
import { CdValues } from './getCdEditInitialValues';
import { Box, Button, IconButton, styled } from '@mui/material';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Plus, Trash01 } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';

const StyledRow = styled('div')`
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: ${({ theme }) => theme.spacing(1)};
  align-items: center;
`;

export const CdCustomMetadata = () => {
  const { values } = useFormikContext<CdValues>();

  return (
    <FieldArray name="customMetadata">
      {({ push, remove }) => (
        <Box display="grid" gap={1}>
          <Box fontWeight="bold" fontSize="0.875rem" color="text.secondary">
            <T keyName="content_delivery_custom_metadata_label" />
          </Box>
          {values.customMetadata.map((_, index) => (
            <StyledRow key={index}>
              <TextField
                size="small"
                name={`customMetadata.${index}.key`}
                label={<T keyName="content_delivery_custom_metadata_key" />}
                minHeight={false}
              />
              <TextField
                size="small"
                name={`customMetadata.${index}.value`}
                label={<T keyName="content_delivery_custom_metadata_value" />}
                minHeight={false}
              />
              <IconButton size="small" onClick={() => remove(index)}>
                <Trash01 width={16} height={16} />
              </IconButton>
            </StyledRow>
          ))}
          <Box>
            <Button
              size="small"
              startIcon={<Plus width={16} height={16} />}
              onClick={() => push({ key: '', value: '' })}
            >
              <T keyName="content_delivery_custom_metadata_add" />
            </Button>
          </Box>
        </Box>
      )}
    </FieldArray>
  );
};
