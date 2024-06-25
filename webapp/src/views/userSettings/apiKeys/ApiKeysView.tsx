import { FunctionComponent, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Link, Route } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { BaseUserSettingsView } from '../BaseUserSettingsView';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { ApiKeyListItem } from './ApiKeyListItem';
import { ApiKeysEmptyListMessage } from './ApiKeysEmptyListMessage';
import { GenerateApiKeyDialog } from './GenerateApiKeyDialog';
import { NewTokenType } from '../pats/PatListItem';
import { components } from 'tg.service/apiSchema.generated';
import { EditApiKeyDialog } from './EditApiKeyDialog';
import { RegenerateApiKeyDialog } from './RegenerateApiKeyDialog';
import { Box, styled } from '@mui/material';

const StyledLink = styled(Link)`
  color: ${({ theme }) => theme.palette.primary.main};
  text-decoration: none;
`;

export const ApiKeysView: FunctionComponent = () => {
  const { t } = useTranslate();
  const [page, setPage] = useState(0);
  const list = useApiQuery({
    url: '/v2/api-keys',
    query: {
      pageable: {
        size: 1000,
        page,
        sort: ['createdAt,desc'],
      },
    },
    method: 'get',
  });

  const [newApiKey, setNewApiKey] = useState({
    type: undefined as NewTokenType,
    apiKey: null as components['schemas']['RevealedApiKeyModel'] | null,
  });

  return (
    <>
      <BaseUserSettingsView
        windowTitle={t('api_keys_title')}
        title={t('api_keys_title')}
        loading={list.isFetching}
        addLabel={t('api_keys_add')}
        navigation={[[t('user_menu_api_keys'), LINKS.USER_API_KEYS.build()]]}
        hideChildrenOnLoading={false}
        addLinkTo={LINKS.USER_API_KEYS_GENERATE.build()}
      >
        <Box sx={{ my: 2 }}>
          <T
            keyName="api-keys-description"
            params={{ link: <StyledLink to={LINKS.USER_PATS.build()} /> }}
          />
        </Box>
        <PaginatedHateoasList
          wrapperComponentProps={{ className: 'listWrapper' }}
          onPageChange={setPage}
          loadable={list}
          renderItem={(apiKey) => {
            const isNew = newApiKey.apiKey?.id === apiKey.id;
            return (
              <ApiKeyListItem
                apiKey={apiKey}
                newTokenType={isNew ? newApiKey.type : undefined}
                newTokenValue={isNew ? newApiKey.apiKey?.key : undefined}
              />
            );
          }}
          emptyPlaceholder={
            <ApiKeysEmptyListMessage loading={list.isFetching} />
          }
        />
      </BaseUserSettingsView>
      <Route exact path={LINKS.USER_API_KEYS_EDIT.template}>
        <EditApiKeyDialog />
      </Route>
      <Route exact path={LINKS.USER_API_KEYS_GENERATE.template}>
        <GenerateApiKeyDialog
          onGenerated={(key) =>
            setNewApiKey({
              type: 'created',
              apiKey: key,
            })
          }
        />
      </Route>
      <Route exact path={LINKS.USER_API_KEYS_REGENERATE.template}>
        <RegenerateApiKeyDialog
          onGenerated={(key) =>
            setNewApiKey({
              type: 'regenerated',
              apiKey: key,
            })
          }
        />
      </Route>
    </>
  );
};
