import React, { useState } from 'react';
import { useIsAdmin, usePreferredOrganization } from 'tg.globalContext/helpers';
import { GlossaryImportDialog } from 'tg.ee.module/glossary/components/GlossaryImportDialog';

export const useGlossaryImportDialog = (hasExistingTerms: boolean) => {
  const { preferredOrganization } = usePreferredOrganization();
  const isUserAdmin = useIsAdmin();

  const [importDialogOpen, setImportDialogOpen] = useState(false);

  const onImport = () => {
    setImportDialogOpen(true);
  };

  const canImport =
    ['OWNER', 'MAINTAINER'].includes(
      preferredOrganization?.currentUserRole || ''
    ) || isUserAdmin;

  const importDialog = canImport &&
    importDialogOpen &&
    preferredOrganization !== undefined && (
      <GlossaryImportDialog
        open={importDialogOpen}
        onClose={() => setImportDialogOpen(false)}
        onFinished={() => setImportDialogOpen(false)}
        hasExistingTerms={hasExistingTerms}
      />
    );

  return {
    onImport,
    importDialogOpen,
    importDialog,
  };
};
