import { EmailNotVerifiedView } from 'tg.component/EmailNotVerifiedView';
import { useIsEmailVerified } from 'tg.globalContext/helpers';
import { ProjectListView } from 'tg.views/projects/ProjectListView';

export const RootView = () => {
  const isEmailVerified = useIsEmailVerified();

  return isEmailVerified ? <ProjectListView /> : <EmailNotVerifiedView />;
};
