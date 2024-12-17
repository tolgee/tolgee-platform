import { Alert, Box, Chip, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { useState } from 'react';
import { CdFileLink } from './CdFileLink';
import { components } from 'tg.service/apiSchema.generated';
import { ChevronDown, ChevronUp } from '@untitled-ui/icons-react';
import { CopyUrlItem } from '../CopyUrlItem';

type ContentDeliveryConfigModel =
  components['schemas']['ContentDeliveryConfigModel'];

const StyledLastPublish = styled('div')`
  display: grid;
  padding: 4px 0px;
  gap: 12px;
  font-size: 14px;
  justify-self: stretch;
`;

const StyledButton = styled(Box)`
  display: flex;
  align-items: center;
  cursor: pointer;
  gap: 4px;
`;

type Props = {
  data: ContentDeliveryConfigModel;
};

export const CdFilesRow = ({ data }: Props) => {
  const [showAllFiles, setShowAllFiles] = useState(false);

  const getFileUrl = (file: string) => {
    return data.publicUrl + '/' + file;
  };

  return (
    <StyledLastPublish>
      <Box display="flex" justifyContent="space-between" gap={2}>
        {Boolean(data.lastPublishedFiles.length) && (
          <StyledButton
            role="button"
            onClick={() => setShowAllFiles(!showAllFiles)}
            data-cy="content-delivery-files-button"
          >
            <Box sx={{ fontWeight: 500 }}>
              <T keyName="content_delivery_last_publish_files" />
            </Box>
            <Chip label={data.lastPublishedFiles.length} size="small" />
            {showAllFiles ? (
              <ChevronUp width={22} height={22} />
            ) : (
              <ChevronDown width={22} height={22} />
            )}
          </StyledButton>
        )}
        <CopyUrlItem
          value={data.publicUrl || ''}
          sx={{ flexGrow: 1 }}
          maxWidth="unset"
        />
      </Box>

      {showAllFiles && (
        <Box display="grid">
          {data.lastPublishedFiles.map((file) => (
            <CdFileLink key={file} link={getFileUrl(file)} file={file} />
          ))}
          <Alert severity="info" sx={{ marginTop: 2 }}>
            <T keyName="content_delivery_missing_files_info" />
          </Alert>
        </Box>
      )}
    </StyledLastPublish>
  );
};
