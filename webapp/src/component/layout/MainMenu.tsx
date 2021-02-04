import {TopBar} from './TopBar';
import {SideMenu} from './sideMenu/SideMenu';
import * as React from 'react';
import {ReactElement} from 'react';
import {Box} from "@material-ui/core";
import {useSelector} from "react-redux";
import {AppState} from "../../store";
import {container} from "tsyringe";
import {GlobalActions} from "../../store/global/globalActions";
import {UserMenu} from "../security/UserMenu";

interface MainMenuProps {
    sideMenuItems?: ReactElement;
    repositoryName?: string
}

const actions = container.resolve(GlobalActions);

export const MainMenu = ({sideMenuItems, ...props}: MainMenuProps) => {

    let open = useSelector((state: AppState) => state.global.sideMenuOpen);

    return (
        <>
            {!props.repositoryName && <TopBar/> ||

            <SideMenu onSideMenuToggle={() => actions.toggleSideMenu.dispatch()} open={open}>
                <Box display="flex" justifyContent="center" mt={2} mb={2} p={1}>
                    <Box display="flex" flexDirection="column">
                        {props.repositoryName &&
                        <Box fontWeight="bold"
                             display="flex"
                             fontSize={open ? 20 : 25}
                             mb={open ? 0 : 2}
                             whiteSpace="normal"
                             justifyContent={!open && "center" || "initial"}>
                            {open && props.repositoryName || props.repositoryName.substr(0, 1)}
                        </Box>}

                        <Box display="flex" justifyContent="center">
                            <UserMenu variant={open ? "expanded" : "small"}/>
                        </Box>
                    </Box>
                </Box>

                {sideMenuItems}
            </SideMenu>
            }
        </>
    )
};