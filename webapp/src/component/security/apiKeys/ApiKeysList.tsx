import {default as React, FunctionComponent} from "react";
import {ApiKeyDTO} from "../../../service/response.types";
import {Box, Paper} from "@material-ui/core";
import {EditIconButton} from "../../common/buttons/EditIconButton";
import {DeleteIconButton} from "../../common/buttons/DeleteIconButton";
import {Link} from "react-router-dom";
import {LINKS, PARAMS} from "../../../constants/links";
import {container} from "tsyringe";
import {UserApiKeysActions} from "../../../store/api_keys/UserApiKeysActions";
import {confirmation} from "../../../hooks/confirmation";
import {T} from "@polygloat/react";

interface ApiKeysListProps {
    data: ApiKeyDTO[]
}

const actions = container.resolve(UserApiKeysActions);

const onDelete = (dto: ApiKeyDTO) => {
    const onConfirm = () => actions.loadableActions.delete.dispatch(dto.key);
    confirmation({title: "Delete api key", message: "Do you really want to delete api key " + dto.key + "?", onConfirm});
};

const Item: FunctionComponent<{ keyDTO: ApiKeyDTO }> = (props) => {
    return (
        <Box mt={1} mb={1}>
            <Paper>
                <Box p={2}>
                    <Box display="flex">
                        <Box flexGrow={1}>
                            <b><T>Api key list label - Api Key</T> {props.keyDTO.key}</b>
                        </Box>
                        <Box>
                            <T>Api key list label - Repository</T> {props.keyDTO.repositoryName}
                        </Box>
                    </Box>
                    <Box display="flex">
                        <Box flexGrow={1} display="flex" alignItems="center">
                            <T>Api key list label - Scopes</T>&nbsp;{props.keyDTO.scopes.join(", ")}
                        </Box>
                        <Box>
                            <EditIconButton component={Link} to={LINKS.USER_API_KEYS_EDIT.build({[PARAMS.API_KEY_ID]: props.keyDTO.id})} size="small"/>
                            <DeleteIconButton onClick={() => onDelete(props.keyDTO)} size="small"/>
                        </Box>
                    </Box>
                </Box>
            </Paper>
        </Box>
    )
};


export const ApiKeysList: FunctionComponent<ApiKeysListProps> = (props) => {
    return (
        <Box>
            {props.data.map(k => (<Item keyDTO={k} key={k.id.toString()}/>))}
        </Box>
    );
};