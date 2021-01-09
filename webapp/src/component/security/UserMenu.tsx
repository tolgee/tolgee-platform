import {default as React, FunctionComponent, useState} from 'react';
import {Button, MenuProps} from '@material-ui/core';
import {container} from 'tsyringe';
import {GlobalActions} from '../../store/global/globalActions';
import {useSelector} from 'react-redux';
import {AppState} from '../../store';
import {useConfig} from "../../hooks/useConfig";
import Menu from "@material-ui/core/Menu";
import MenuItem from "@material-ui/core/MenuItem";
import {useUser} from "../../hooks/useUser";
import {Link} from "react-router-dom";
import {LINKS} from "../../constants/links";
import KeyboardArrowDownIcon from '@material-ui/icons/KeyboardArrowDown';
import PersonIcon from '@material-ui/icons/Person';
import withStyles from "@material-ui/core/styles/withStyles";

interface UserMenuProps {
    variant: "small" | "expanded"
}

const globalActions = container.resolve(GlobalActions);

export const UserMenu: FunctionComponent<UserMenuProps> = (props) => {

    const userLogged = useSelector((state: AppState) => state.global.security.allowPrivate);

    const authentication = useConfig().authentication;

    const handleOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };


    const [anchorEl, setAnchorEl] = useState(null);

    const user = useUser();

    if (!authentication || !user) {
        return null;
    }

    const StyledMenu = withStyles({
        paper: {
            border: '1px solid #d3d4d5',
        },
    })((props: MenuProps) => (
        <Menu
            elevation={0}
            getContentAnchorEl={null}
            anchorOrigin={{
                vertical: 'bottom',
                horizontal: 'right',
            }}
            transformOrigin={{
                vertical: 'top',
                horizontal: 'right',
            }}
            {...props}
        />
    ));

    return (
        <>
            {userLogged &&
            <div>
                <Button style={{padding: 0}} endIcon={<KeyboardArrowDownIcon/>} color="inherit" aria-controls="user-menu" aria-haspopup="true"
                        onClick={handleOpen}>{props.variant == "expanded" ? user.name : <PersonIcon/>}</Button>
                <StyledMenu id="user-menu" keepMounted
                            open={!!anchorEl}
                            anchorEl={anchorEl}
                            onClose={handleClose}
                >
                    <MenuItem onClick={() => globalActions.logout.dispatch()}>Logout</MenuItem>
                    <MenuItem component={Link} to={LINKS.USER_SETTINGS.build()}>Settings</MenuItem>
                    <MenuItem component={Link} to={LINKS.USER_API_KEYS.build()}>Api keys</MenuItem>
                </StyledMenu>
            </div>
            }
        </>
    );
};
