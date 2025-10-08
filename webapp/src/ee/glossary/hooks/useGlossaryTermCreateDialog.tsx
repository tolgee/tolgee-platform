import { GlossaryTermCreateDialog } from 'tg.ee.module/glossary/views/GlossaryTermCreateDialog';
import React, { useState } from 'react';
import { useIsAdmin, usePreferredOrganization } from 'tg.globalContext/helpers';

export const useGlossaryTermCreateDialog = () => {
  const { preferredOrganization } = usePreferredOrganization();
  const isUserAdmin = useIsAdmin();

  const [createTermDialogOpen, setCreateTermDialogOpen] = useState(false);

  const onCreateTerm = () => {
    setCreateTermDialogOpen(true);
  };

  const canCreate =
    ['OWNER', 'MAINTAINER'].includes(
      preferredOrganization?.currentUserRole || ''
    ) || isUserAdmin;

  const createTermDialog = canCreate &&
    createTermDialogOpen &&
    preferredOrganization !== undefined && (
      <GlossaryTermCreateDialog
        open={createTermDialogOpen}
        onClose={() => setCreateTermDialogOpen(false)}
        onFinished={() => setCreateTermDialogOpen(false)}
      />
    );

  return {
    onCreateTerm,
    createTermDialogOpen,
    createTermDialog,
  };
};
