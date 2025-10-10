import { GlossaryTermCreateDialog } from 'tg.ee.module/glossary/views/GlossaryTermCreateDialog';
import React, { useState } from 'react';
import { useIsAdmin, usePreferredOrganization } from 'tg.globalContext/helpers';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';

export const useGlossaryTermCreateDialog = () => {
  const { preferredOrganization } = usePreferredOrganization();
  const glossary = useGlossary();
  const isUserAdmin = useIsAdmin();
  const isUserMaintainer = ['OWNER', 'MAINTAINER'].includes(
    preferredOrganization?.currentUserRole || ''
  );
  const isGlossaryUnderPreference =
    glossary.organizationOwner.id === preferredOrganization?.id;

  const [createTermDialogOpen, setCreateTermDialogOpen] = useState(false);

  const onCreateTerm = () => {
    setCreateTermDialogOpen(true);
  };

  const canCreate =
    isGlossaryUnderPreference && (isUserMaintainer || isUserAdmin);

  const isOpen = createTermDialogOpen && canCreate;

  const createTermDialog = isOpen && (
    <GlossaryTermCreateDialog
      open={createTermDialogOpen}
      onClose={() => setCreateTermDialogOpen(false)}
      onFinished={() => setCreateTermDialogOpen(false)}
    />
  );

  return {
    onCreateTerm: canCreate ? onCreateTerm : undefined,
    createTermDialogOpen: isOpen,
    createTermDialog,
  };
};
