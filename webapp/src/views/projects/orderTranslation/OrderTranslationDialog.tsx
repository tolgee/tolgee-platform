import { Dialog, DialogContent, DialogTitle, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useState } from 'react';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import { CompactProjectItem } from './CompactProjectItem';
import { ProviderType } from './types';

const StyledDescription = styled('div')`
  margin-bottom: 16px;
`;

const StyledDialogContent = styled(DialogContent)`
  width: 900px;
  max-width: 90vw;
  container: main-container / inline-size;
`;

type Props = {
  provider: ProviderType;
  onClose: () => void;
};

export const OrderTranslationDialog = ({ provider, onClose }: Props) => {
  const [page, setPage] = useState(0);
  const { preferredOrganization } = usePreferredOrganization();

  const listPermitted = useApiQuery({
    url: '/v2/organizations/{slug}/projects-with-stats',
    method: 'get',
    path: { slug: preferredOrganization?.slug || '' },
    query: {
      page,
      size: 20,
      sort: ['id,desc'],
    },
    options: {
      keepPreviousData: true,
      enabled: Boolean(preferredOrganization?.slug),
    },
  });

  const { t } = useTranslate();
  return (
    <Dialog open={true} onClose={onClose} maxWidth="md">
      <DialogTitle>{t('order_translation_dialog_title')}</DialogTitle>

      <StyledDialogContent>
        <StyledDescription>
          {t('order_translation_dialog_subtitle')}
        </StyledDescription>

        <PaginatedHateoasList
          onPageChange={setPage}
          loadable={listPermitted}
          renderItem={(r) => <CompactProjectItem key={r.id} {...r} />}
        />
      </StyledDialogContent>
    </Dialog>
  );
};
