import {BaseView, BaseViewProps} from "../../layout/BaseView";
import * as React from "react";
import {FunctionComponent, PropsWithChildren} from "react";
import {Box, Grid, Typography} from "@material-ui/core";
import {OrganizationSettingsMenu} from "./components/OrganizationSettingsMenu";
import {useOrganization} from "../../../hooks/organizations/useOrganization";
import UserOrganizationSettingsSubtitleLink from "./components/UserOrganizationSettingsSubtitleLink";
import {container} from "tsyringe";
import {OrganizationActions} from "../../../store/organization/OrganizationActions";
import SearchField from "../../common/form/fields/SearchField";

const actions = container.resolve(OrganizationActions);

export const BaseOrganizationSettingsView: FunctionComponent<BaseViewProps> = ({children, title, ...otherProps}: PropsWithChildren<BaseViewProps>) => {

    const organization = useOrganization();

    const organizationLoadable = actions.useSelector(state => state.loadables.get);

    return (
        <BaseView {...otherProps} containerMaxWidth="md"
                  loading={organizationLoadable.loading}
                  customHeader={
                      <>
                          <Typography variant="h5">{organization?.name}</Typography>
                          <UserOrganizationSettingsSubtitleLink isUser={false}/>
                      </>
                  }
                  hideChildrenOnLoading={false}
        >
            <Grid container>
                <Grid item lg={3} md={4}>
                    <Box mr={4} mb={4}>
                        <OrganizationSettingsMenu/>
                    </Box>
                </Grid>
                <Grid item lg={9} md={8} sm={12} xs={12}>
                    {title && <Box mb={2}>
                        <Box><Typography variant="h6">{title}</Typography></Box>
                    </Box>}
                    {children}
                </Grid>
            </Grid>
        </BaseView>

    )

}
