import {BaseView, BaseViewProps} from "../../layout/BaseView";
import * as React from "react";
import {FunctionComponent, PropsWithChildren} from "react";
import {Box, Grid, Typography} from "@material-ui/core";
import {OrganizationSettingsMenu} from "./OrganizationSettingsMenu";
import {useOrganization} from "../../../hooks/organizations/useOrganization";


export const BaseOrganizationSettingsView: FunctionComponent<BaseViewProps> = ({children, title, ...otherProps}: PropsWithChildren<BaseViewProps>) => {

    const organization = useOrganization();

    return (
            <BaseView {...otherProps} title={organization?.name} containerMaxWidth="md">
                <Grid container>
                    <Grid item lg={3} md={4}>
                        <Box mr={4} mb={4}>
                            <OrganizationSettingsMenu/>
                        </Box>
                    </Grid>
                    <Grid item lg={9} md={8} sm={12} xs={12}>
                        <Box>
                            <Box mb={2}>
                                <Typography variant="h6">{title}</Typography>
                            </Box>
                            {children}
                        </Box>
                    </Grid>
                </Grid>
            </BaseView>

    )

}
