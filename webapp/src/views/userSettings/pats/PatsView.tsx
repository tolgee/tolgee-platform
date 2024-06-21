import { FunctionComponent, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Link, Route } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';

import { BaseUserSettingsView } from '../BaseUserSettingsView';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { GeneratePatDialog } from './GeneratePatDialog';
import { PatEmptyListMessage } from './PatEmptyListMessage';
import { components } from 'tg.service/apiSchema.generated';
import { NewTokenType, PatListItem } from './PatListItem';
import { RegeneratePatDialog } from './RegeneratePatDialog';
import { EditPatDialog } from './EditPatDialog';
import { Box, styled } from '@mui/material';

const StyledLink = styled(Link)`
  color: ${({ theme }) => theme.palette.primary.main};
  text-decoration: none;
`;

export const PatsView: FunctionComponent = () => {
  const { t } = useTranslate();
  const [page, setPage] = useState(0);

  const list = useApiQuery({
    url: '/v2/pats',
    query: {
      size: 50,
      page: page,
      sort: ['createdAt,desc'],
    },
    method: 'get',
  });
  const [newToken, setNewToken] = useState({
    type: undefined as NewTokenType,
    pat: null as components['schemas']['RevealedPatModel'] | null,
  });

  return (
    <>
      <BaseUserSettingsView
        windowTitle={t('pats_title')}
        title={t('pats_title')}
        addLabel={t('pats_add')}
        loading={list.isFetching}
        navigation={[[t('user_menu_pats'), LINKS.USER_PATS.build()]]}
        hideChildrenOnLoading={false}
        addLinkTo={LINKS.USER_PATS_GENERATE.template}
      >
        <Box sx={{ my: 2 }}>
          <T
            keyName="pats-description"
            params={{
              link: <StyledLink to={LINKS.USER_API_KEYS.build()} />,
            }}
          />
        </Box>
        <PaginatedHateoasList
          wrapperComponentProps={{ className: 'listWrapper' }}
          onPageChange={setPage}
          loadable={list}
          renderItem={(pat) => {
            const isNew = newToken.pat?.id === pat.id;
            return (
              <PatListItem
                pat={pat}
                newTokenValue={isNew ? newToken?.pat?.token : undefined}
                newTokenType={isNew ? newToken.type : undefined}
              />
            );
          }}
          emptyPlaceholder={<PatEmptyListMessage loading={list.isFetching} />}
        />

        <Route exact path={LINKS.USER_PATS_GENERATE.template}>
          <GeneratePatDialog
            onGenerated={(pat) => setNewToken({ pat: pat, type: 'created' })}
          />
        </Route>
        <Route exact path={LINKS.USER_PATS_REGENERATE.template}>
          <RegeneratePatDialog
            onGenerated={(pat) =>
              setNewToken({ pat: pat, type: 'regenerated' })
            }
          />
        </Route>
        <Route exact path={LINKS.USER_PATS_EDIT.template}>
          <EditPatDialog />
        </Route>
      </BaseUserSettingsView>
    </>
  );
};
