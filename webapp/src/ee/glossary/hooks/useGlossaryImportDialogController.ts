import { useState } from 'react';
import { useIsAdmin, usePreferredOrganization } from 'tg.globalContext/helpers';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';

export const useGlossaryImportDialogController = () => {
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

  return {
    onImport: canImport ? onImport : undefined,
    importDialogOpen: isOpen,
    importDialogProps: {
      open: isOpen,
      onClose: () => setImportDialogOpen(false),
      onFinished: () => setImportDialogOpen(false),
    },
  };
};
