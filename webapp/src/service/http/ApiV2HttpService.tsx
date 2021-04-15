import {singleton} from 'tsyringe';
import {TokenService} from '../TokenService';
import {MessageService} from "../MessageService";
import React from "react";
import {ApiV1HttpService} from "./ApiV1HttpService";
import {RedirectionActions} from "../../store/global/RedirectionActions";

@singleton()
export class ApiV2HttpService extends ApiV1HttpService {
    constructor(tokenService: TokenService, messageService: MessageService, redirectionActions: RedirectionActions) {
        super(tokenService, messageService, redirectionActions)
    }

    apiUrl = process.env.REACT_APP_API_URL + "/v2/"
}
