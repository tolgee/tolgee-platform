import * as React from 'react';
import {useEffect} from 'react';
import {container} from 'tsyringe';
import {BaseView} from '../../layout/BaseView';
import {useTranslate} from "@tolgee/react";
import {DashboardPage} from "../../layout/DashboardPage";
import {OrganizationActions} from "../../../store/organization/OrganizationActions";
import {SimplePaginatedList} from "../../common/list/SimplePaginatedList";
import {ListItemLink} from "../../common/list/ListItemLink";
import {LINKS, PARAMS} from "../../../constants/links";
import ListItemText from "@material-ui/core/ListItemText";
import ListItemSecondaryAction from "@material-ui/core/ListItemSecondaryAction";
import {Link} from "react-router-dom";
import {SettingsIconButton} from "../../common/buttons/SettingsIconButton";
import {OrganizationRoleType} from "../../../service/response.types";
import {FabAddButtonLink} from "../../common/buttons/FabAddButtonLink";
import Box from "@material-ui/core/Box";

const actions = container.resolve(OrganizationActions);

export const OrganizationsListView = () => {

    const organizationsLoadable = actions.useSelector((state) => state.loadables.listPermitted)

    const t = useTranslate();

    useEffect(() => {
        actions.loadableActions.listPermitted.dispatch()
    }, [])

    return (
        <DashboardPage>
            <BaseView title={t("organizations_title")} lg={5} md={7} loading={organizationsLoadable.loading}>
                <SimplePaginatedList
                    data={organizationsLoadable.data!!}
                    renderItem={(item) =>
                        <ListItemLink
                            key={item.id}
                            to={LINKS.REPOSITORY_TRANSLATIONS.build({[PARAMS.REPOSITORY_ID]: item.id})}
                        >
                            <ListItemText>
                                {item.name}
                            </ListItemText>
                            {item.currentUserRole == OrganizationRoleType.OWNER &&
                            <ListItemSecondaryAction>
                                <Link to={LINKS.REPOSITORY_EDIT.build({[PARAMS.REPOSITORY_ID]: item.id})}>
                                    <SettingsIconButton/>
                                </Link>
                            </ListItemSecondaryAction>}
                        </ListItemLink>
                    }/>
                <Box display="flex" flexDirection="column" alignItems="flex-end" mt={2} pr={2}>
                    <FabAddButtonLink to={LINKS.ORGANIZATIONS_ADD.build()}/>
                </Box>
            </BaseView>
        </DashboardPage>
    );
}

