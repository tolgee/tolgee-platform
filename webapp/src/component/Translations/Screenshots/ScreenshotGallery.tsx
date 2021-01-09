import * as React from 'react';
import {FunctionComponent, ReactNode} from 'react';
import {BoxLoading} from "../../common/BoxLoading";
import {ScreenshotThumbnail} from "./ScreenshotThumbnail";
import Box from '@material-ui/core/Box';

import {KeyTranslationsDTO, RepositoryPermissionType, ScreenshotDTO} from "../../../service/response.types";
import {ScreenshotActions} from "../../../store/repository/ScreenshotActions";
import {container} from 'tsyringe';
import AddIcon from '@material-ui/icons/Add';
import {Message} from "../../../store/global/types";
import {T} from '@polygloat/react';
import {MessageActions} from "../../../store/global/messageActions";
import {useConfig} from "../../../hooks/useConfig";
import {useRepository} from "../../../hooks/useRepository";
import {createStyles, makeStyles, Theme} from '@material-ui/core';
import {ScreenshotDetail} from "./ScreenshotDetail";
import {ScreenshotDropzone} from "./ScreenshotDropzone";
import {useRepositoryPermissions} from "../../../hooks/useRepositoryPermissions";

export interface ScreenshotGalleryProps {
    data: KeyTranslationsDTO,
}

const useStyles = makeStyles((theme: Theme) => createStyles({
    addIcon: {
        fontSize: 50,
    },
    addBox: {
        overflow: "hidden",
        width: "100px",
        height: "100px",
        alignItems: "center",
        justifyContent: "center",
        display: "flex",
        margin: "1px",
        cursor: "pointer",
        borderColor: theme.palette.grey[200],
        color: theme.palette.grey[200],
        border: `1px dashed ${theme.palette.grey[200]}`,
        "&:hover": {
            borderColor: theme.palette.primary.main,
            color: theme.palette.primary.main,
        },
        flex: "0 0 auto"
    },
}));

const actions = container.resolve(ScreenshotActions)
const messageActions = container.resolve(MessageActions)
export const MAX_FILE_COUNT = 20;
const ALLOWED_UPLOAD_TYPES = ["image/png", "image/jpeg", "image/gif"];

export const ScreenshotGallery: FunctionComponent<ScreenshotGalleryProps> = (props) => {
        const fileRef = React.createRef<HTMLInputElement>();
        const screenshotsLoadable = actions.useSelector(s => s.loadables.getForKey)
        const screenshots = screenshotsLoadable.data as ScreenshotDTO[]
        const repositoryPermissions = useRepositoryPermissions();

        const [detailFileName, setDetailFileName] = React.useState(null);
        const classes = useStyles({});
        const config = useConfig();
        const repository = useRepository();

        const addBox = repositoryPermissions.satisfiesPermission(RepositoryPermissionType.TRANSLATE) && (
            <Box key="add" className={`${classes.addBox}`} data-cy="add-box"
                 onClick={() => fileRef.current.dispatchEvent(new MouseEvent("click"))}>
                <AddIcon className={classes.addIcon}/>
            </Box>
        );

        const validate = (files: File[]) => {


            const result = {
                valid: false,
                errors: [] as ReactNode[],
            }

            if (files.length > MAX_FILE_COUNT) {
                result.errors.push(<T>translations.screenshots.validation.too_many_files</T>)
            }

            files.forEach(file => {
                if (file.size > config.maxUploadFileSize * 1024) {
                    result.errors.push(<T parameters={{filename: file.name}}>translations.screenshots.validation.file_too_big</T>)
                }
                if (ALLOWED_UPLOAD_TYPES.indexOf(file.type) < 0) {
                    result.errors.push(<T parameters={{filename: file.name}}>translations.screenshots.validation.unsupported_format</T>)
                }
            })

            const valid = result.errors.length === 0;
            return {...result, valid};
        }

        const validateAndUpload = (files: File[]) => {
            const validation = validate(files);
            if (validation.valid) {
                actions.loadableActions.uploadScreenshot.dispatch(files, repository.id, props.data.name)
                return;
            }
            validation.errors.forEach(e => messageActions.showMessage.dispatch(new Message(e, "error")));
        }

        React.useEffect(() => {
            const listener = (e) => {
                e.preventDefault();
            }

            const pasteListener = (e: ClipboardEvent) => {
                const files: File[] = [];
                for (let i = 0; i < e.clipboardData.files.length; i++) {
                    files.push(e.clipboardData.files.item(i));
                }
                validateAndUpload(files);
            }

            window.addEventListener("dragover", listener, false);
            window.addEventListener("drop", listener, false);
            document.addEventListener("paste", pasteListener)

            return () => {
                window.removeEventListener("dragover", listener, false);
                window.removeEventListener("drop", listener, false);
                document.removeEventListener("paste", pasteListener)
            }
        }, [])

        function onFileSelected(e: React.SyntheticEvent) {
            const files = (e.target as HTMLInputElement).files
            const toUpload: File[] = []
            for (let i = 0; i < files.length; i++) {
                toUpload.push(files.item(i))
            }
            validateAndUpload(toUpload);
        }

        return (
            <>
                <input type="file" style={{display: "none"}} ref={fileRef} onChange={e => onFileSelected(e)} multiple accept={ALLOWED_UPLOAD_TYPES.join(",")}/>
                <ScreenshotDropzone validateAndUpload={validateAndUpload}>
                    {screenshotsLoadable.loading || !screenshotsLoadable.touched ? <BoxLoading/>
                        :
                        screenshots.length > 0 ?
                            <Box display="flex" flexWrap="wrap" overflow="visible"> {
                                screenshots.map(s =>
                                    <ScreenshotThumbnail key={s.id} onClick={() => setDetailFileName(s.filename)} screenshotData={s}/>
                                )}
                                {addBox}
                            </Box>
                            :
                            <>
                                <Box display="flex" alignItems="center" justifyContent="center" flexGrow={1} p={2}>
                                    <Box><T>no_screenshots_yet</T></Box>
                                </Box>
                                {addBox}
                            </>}
                </ScreenshotDropzone>
                <ScreenshotDetail fileName={detailFileName} onClose={() => setDetailFileName(null)}/>
            </>
        )
    }
;