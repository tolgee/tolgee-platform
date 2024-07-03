import { FC, useState } from 'react';
import { Box, FormControl, MenuItem, Select, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Plus } from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { useProject } from 'tg.hooks/useProject';
import { GenerateApiKeyDialog } from '../../../userSettings/apiKeys/GenerateApiKeyDialog';

const StyledItemWrapper = styled('div')`
  max-width: 400;

  &.addItem {
    display: flex;
    align-items: center;
    color: ${({ theme }) => theme.palette.primary.main};
    padding-top: ${({ theme }) => theme.spacing(1)};
    padding-bottom: ${({ theme }) => theme.spacing(1)};
  }
`;

const StyledScopes = styled('div')`
  font-size: 11px;
  white-space: normal;
  max-width: 400px;
  font-style: italic;
`;

const StyledAddIcon = styled(Plus)`
  margin-left: ${({ theme }) => theme.spacing(0.5)};
`;

export const ApiKeySelector: FC<{
  selected: components['schemas']['ApiKeyModel'] | undefined;
  onSelect: (key: components['schemas']['ApiKeyModel']) => void;
  keys?: components['schemas']['ApiKeyModel'][];
  keysLoading: boolean;
  onNewCreated: (key: components['schemas']['ApiKeyModel']) => void;
}> = (props) => {
  const [addDialogOpen, setAddDialogOpen] = useState(false);

  const findKey = (id: number) => props.keys?.find((k) => k.id === id);

  const onSelect = (id: number) => {
    if (id === 0) {
      setAddDialogOpen(true);
      return;
    }
    props.onSelect(findKey(id)!);
  };

  const project = useProject();

  const { t } = useTranslate();

  return (
    <>
      {!props.keysLoading ? (
        <FormControl variant="outlined" style={{ minWidth: 400 }}>
          <Select
            data-cy="integrate-api-key-selector-select"
            id="api-key-select"
            value={props.selected?.id || ''}
            inputProps={{
              'data-cy': 'integrate-api-key-selector-select-input',
            }}
            renderValue={(id) => {
              return (
                <span data-sentry-mask="">
                  {findKey(id as number)?.description}
                </span>
              );
            }}
            onChange={(e) => onSelect(e.target.value as number)}
          >
            {props.keys?.map((k) => (
              <MenuItem
                key={k.id}
                value={k.id}
                data-cy="integrate-api-key-selector-item"
              >
                <StyledItemWrapper>
                  <Box data-sentry-mask="">{k.description}</Box>
                  <StyledScopes>{k.scopes.join(', ')}</StyledScopes>
                </StyledItemWrapper>
              </MenuItem>
            ))}
            <MenuItem
              value={0}
              data-cy="integrate-api-key-selector-create-new-item"
            >
              <StyledItemWrapper className="addItem">
                <T keyName="api_key_selector_create_new" />
                <StyledAddIcon width={20} height={20} />
              </StyledItemWrapper>
            </MenuItem>
          </Select>
        </FormControl>
      ) : (
        <BoxLoading p={1} />
      )}
      {addDialogOpen && (
        <GenerateApiKeyDialog
          project={project}
          initialDescriptionValue={t(
            'integrate-initial-api-key-description-value'
          )}
          onClose={() => {
            setAddDialogOpen(false);
          }}
          onGenerated={(key) => {
            setAddDialogOpen(false);
            props.onNewCreated(key);
          }}
        />
      )}
    </>
  );
};
