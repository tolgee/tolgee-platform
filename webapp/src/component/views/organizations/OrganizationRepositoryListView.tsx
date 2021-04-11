import * as React from 'react';
import {container} from 'tsyringe';
import {useTranslate} from "@tolgee/react";
import {OrganizationActions} from "../../../store/organization/OrganizationActions";
import {BaseView} from "../../layout/BaseView";
import RepositoryListItem from "../repositories/RepositoryListItem";
import {useOrganization} from "../../../hooks/organizations/useOrganization";
import {SimplePaginatedHateoasList} from "../../common/list/SimplePaginatedHateoasList";
import Box from "@material-ui/core/Box";
import {FabAddButtonLink} from "../../common/buttons/FabAddButtonLink";
import {LINKS} from "../../../constants/links";

const actions = container.resolve(OrganizationActions);

export const OrganizationsRepositoryListView = () => {

    const t = useTranslate();

    const organization = useOrganization();

    const loadable = actions.useSelector(state => state.loadables.listRepositories)

    return (
        <BaseView title={t("organization_repositories_title")} containerMaxWidth="md" hideChildrenOnLoading={false} loading={loadable.loading}>
            <SimplePaginatedHateoasList pageSize={20} dispatchParams={[organization.addressPart]} actions={actions} loadableName="listRepositories" renderItem={r =>
                <RepositoryListItem {...r} />
            }/>
            <Box display="flex" flexDirection="column" alignItems="flex-end" mt={2} pr={2}>
                <FabAddButtonLink to={LINKS.REPOSITORY_ADD.build()}/>
            </Box>
        </BaseView>
    );
}

