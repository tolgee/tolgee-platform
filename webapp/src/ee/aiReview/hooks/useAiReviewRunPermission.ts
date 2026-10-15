import { useSatisfiesPermissionWithBranching } from 'tg.hooks/useSatisfiesPermissionWithBranching';

export const useAiReviewRunPermission = () => {
  const satisfiesPermission = useSatisfiesPermissionWithBranching();
  return (
    satisfiesPermission('translations.state-edit') &&
    satisfiesPermission('translations.batch-machine')
  );
};
