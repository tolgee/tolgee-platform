import {DashboardPage} from "../../layout/DashboardPage";
import {BaseView, BaseViewProps} from "../../layout/BaseView";
import * as React from "react";
import {FunctionComponent, PropsWithChildren} from "react";
import {Box, Grid, Typography} from "@material-ui/core";
import {useUser} from "../../../hooks/useUser";
import {UserSettingsMenu} from "./UserSettingsMenu";


export const BaseUserSettingsView: FunctionComponent<BaseViewProps> = ({children, title, ...otherProps}: PropsWithChildren<BaseViewProps>) => {

    const user = useUser()

    return (
        <DashboardPage>
            <BaseView {...otherProps} title={user?.name} containerMaxWidth="md">
                <Grid container>
                    <Grid item lg={3} md={4}>
                        <Box mr={4} mb={4}>
                            <UserSettingsMenu/>
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
        </DashboardPage>

    )

}
