import {default as React, FunctionComponent, useState} from 'react';
import {Button, MenuProps} from '@material-ui/core';
import Menu from "@material-ui/core/Menu";
import MenuItem from "@material-ui/core/MenuItem";
import KeyboardArrowDownIcon from '@material-ui/icons/KeyboardArrowDown';
import withStyles from "@material-ui/core/styles/withStyles";
import LanguageIcon from '@material-ui/icons/Language';
import {useCurrentLanguage, useSetLanguage} from "@polygloat/react";

export const LocaleMenu: FunctionComponent<{ className?: string }> = (props) => {
    const handleOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    const [anchorEl, setAnchorEl] = useState(null);

    const setLanguage = useSetLanguage();
    const getCurrentLanguage = useCurrentLanguage();

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

    const languages = {
        en: "English",
        cz: "Česky"
    }

    return (
        <>
            <div>
                <Button style={{padding: 0}} endIcon={<KeyboardArrowDownIcon/>}
                        color="inherit"
                        aria-controls="language-menu" aria-haspopup="true"
                        onClick={handleOpen}><LanguageIcon/>
                </Button>
                <StyledMenu id="language-menu" keepMounted
                            open={!!anchorEl}
                            anchorEl={anchorEl}
                            onClose={handleClose}
                            onChange={e => console.log(e)}
                            {...props}
                >{
                    Object.entries(languages).map(([abbr, name]) =>
                        <MenuItem
                            selected={getCurrentLanguage() === abbr}
                            value={abbr}
                            key={abbr}
                            onClick={() => {
                                handleClose();
                                setLanguage(abbr);
                            }}>{name}</MenuItem>)
                }
                </StyledMenu>
            </div>
        </>
    );
};
