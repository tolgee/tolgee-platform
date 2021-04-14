import {default as React, FunctionComponent} from "react";
import {Alert} from "../Alert";
import {ErrorResponseDTO} from "../../../service/response.types";
import {T} from "@tolgee/react";
import {Box} from "@material-ui/core";
import { parseErrorResponse } from "../../../fixtures/errorFIxtures";

export const ResourceErrorComponent: FunctionComponent<{ error: ErrorResponseDTO | any }> = (props) => {
    return <>{props.error && parseErrorResponse(props.error).map(e => (
        <Box ml={-2} mr={-2} key={new Date().toDateString()}>
            <Alert severity="error">
                <T>{e}</T>
            </Alert>
        </Box>))}</>;
};
