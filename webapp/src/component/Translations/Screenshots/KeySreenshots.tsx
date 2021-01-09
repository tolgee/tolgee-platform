import * as React from 'react';
import {FunctionComponent} from 'react';
import PhotoCameraIcon from '@material-ui/icons/PhotoCamera';
import {T} from '@polygloat/react';
import {Box, createStyles, IconButton, makeStyles, Theme, Tooltip} from '@material-ui/core';
import {KeyTranslationsDTO} from "../../../service/response.types";
import {ScreenshotsPopover} from "./ScreenshotsPopover";

export interface ScreenshotsProps {
    data: KeyTranslationsDTO
}

const useStyles = makeStyles((theme: Theme) => createStyles({
    cameraButton: {
        opacity: "0.2",
        padding: 0,
        '&:hover': {
            opacity: 1
        }
    },
    screenshot: {
        maxWidth: "100%",
        maxHeight: "100%"
    },
    screenshotBox: {
        width: "100px",
        height: "100px",
        alignItems: "center",
        justifyContent: "center",
        display: "flex",
        margin: theme.spacing(1),
        border: `1px solid ${theme.palette.grey[100]}`
    }
}));


export const KeyScreenshots: FunctionComponent<ScreenshotsProps> = (props) => {
    const [anchorEl, setAnchorEl] = React.useState(null);

    const handleClick = (event) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    const classes = useStyles({});

    return (
        <>
            <Box display="flex" alignItems="center">
                <IconButton className={classes.cameraButton} data-cy="camera-button" onClick={handleClick}>
                    <Tooltip title={<T noWrap>translation_grid_screenshots_tooltip</T>}>
                        <PhotoCameraIcon/>
                    </Tooltip>
                </IconButton>
            </Box>
            {!!anchorEl &&
            <ScreenshotsPopover data={props.data} anchorEl={anchorEl} onClose={handleClose}/>
            }
        </>
    )
};