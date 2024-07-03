import { Box } from '@mui/material';
import { useHistory } from 'react-router-dom';
import { Usage } from 'tg.component/billing/Usage';
import { BaseView, BaseViewProps } from 'tg.component/layout/BaseView';
import { NavigationItem } from 'tg.component/navigation/Navigation';
import { SmallProjectAvatar } from 'tg.component/navigation/SmallProjectAvatar';
import { OrganizationSwitch } from 'tg.component/organizationSwitch/OrganizationSwitch';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { BatchOperationsSummary } from './translations/BatchOperations/OperationsSummary/OperationsSummary';

type Props = BaseViewProps;

export const BaseProjectView: React.FC<Props> = ({
  navigation,
  ...otherProps
}) => {
  const project = useProject() as ReturnType<typeof useProject> | undefined;

  const history = useHistory();

  const handleOrganizationChange = () => {
    history.push(LINKS.PROJECTS.build());
  };

  if (!project) {
    return <BaseView {...otherProps}></BaseView>;
  }

  const prefixNavigation: NavigationItem[] = [
    [<OrganizationSwitch key={0} onSelect={handleOrganizationChange} />],
    [
      project.name,
      LINKS.PROJECT_DASHBOARD.build({
        [PARAMS.PROJECT_ID]: project.id,
      }),
      <SmallProjectAvatar key={0} project={project} />,
    ],
  ];
  return (
    <BaseView
      {...otherProps}
      navigation={[...prefixNavigation, ...(navigation || [])]}
      navigationRight={
        <Box display="grid">
          <BatchOperationsSummary />
          <Usage />
        </Box>
      }
    />
  );
};
