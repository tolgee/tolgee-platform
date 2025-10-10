import React, { useState } from 'react';
import { useIsAdmin, usePreferredOrganization } from 'tg.globalContext/helpers';
import { GlossaryImportDialog } from 'tg.ee.module/glossary/components/GlossaryImportDialog';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';

export const useGlossaryImportDialog = (hasExistingTerms: boolean) => {
  const { preferredOrganization } = usePreferredOrganization();
  const glossary = useGlossary();
  const isUserAdmin = useIsAdmin();
  const isUserMaintainerOrOwner = ['OWNER', 'MAINTAINER'].includes(
    preferredOrganization?.currentUserRole || ''
  );
  const isGlossaryUnderPreference =
    glossary.organizationOwner.id === preferredOrganization?.id;

  const [importDialogOpen, setImportDialogOpen] = useState(false);

  const onImport = () => {
    setImportDialogOpen(true);
  };

  const canImport =
    isGlossaryUnderPreference && (isUserMaintainerOrOwner || isUserAdmin);

  const isOpen = importDialogOpen && canImport;

  const importDialog = isOpen && (
    <GlossaryImportDialog
      open={importDialogOpen}
      onClose={() => setImportDialogOpen(false)}
      onFinished={() => setImportDialogOpen(false)}
      hasExistingTerms={hasExistingTerms}
    />
  );

  return {
    onImport: canImport ? onImport : undefined,
    importDialogOpen: isOpen,
    importDialog,
  };
};
