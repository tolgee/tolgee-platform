import * as React from 'react';
import {FunctionComponent, useEffect} from 'react';
import {Box, Popover, Typography} from '@material-ui/core';
import {KeyTranslationsDTO} from "../../../service/response.types";
import {ScreenshotActions} from "../../../store/repository/ScreenshotActions";
import {container} from 'tsyringe';
import {useRepository} from "../../../hooks/useRepository";
import {T} from '@polygloat/react';
import {Alert} from "../../common/Alert";
import {ScreenshotGallery} from "./ScreenshotGallery";

export interface ScreenshotsPopoverProps {
    data: KeyTranslationsDTO,
    anchorEl: Element,
    onClose: () => void
}


const actions = container.resolve(ScreenshotActions)
export const ScreenshotsPopover: FunctionComponent<ScreenshotsPopoverProps> = (props) => {
    const uploadLoadable = actions.useSelector(s => s.loadables.uploadScreenshot)

    const repository = useRepository();
    const id = open ? `screenshot-popover-${props.data.id}` : undefined;

    useEffect(() => {
        actions.loadableActions.getForKey.dispatch(repository.id, props.data.name)
        return () => {
            actions.loadableReset.uploadScreenshot.dispatch()
        }
    }, [])

    return (
        <>
            <Popover
                id={id}
                open={true}
                anchorEl={props.anchorEl}
                onClose={props.onClose}
                anchorOrigin={{
                    vertical: 'bottom',
                    horizontal: 'center',
                }}
                transformOrigin={{
                    vertical: 'top',
                    horizontal: 'center',
                }}
            >
                <Box width="408px">
                    <Box p={2}>
                        <Typography><T>translations_screenshots_popover_title</T></Typography>
                    </Box>

                    {!!uploadLoadable?.data?.errors?.length &&
                    <Box>
                        <Alert severity="error" style={{marginTop: 0, width: "100%"}}><T>translations.screenshots.some_screenshots_not_uploaded</T></Alert>
                    </Box>
                    }
                    <ScreenshotGallery data={props.data}/>
                </Box>
            </Popover>
        </>
    )
};