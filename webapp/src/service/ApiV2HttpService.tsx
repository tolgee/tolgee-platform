import {singleton} from 'tsyringe';
import {TokenService} from './TokenService';
import {MessageService} from "./MessageService";
import React from "react";
import {ApiHttpService} from "./ApiHttpService";

@singleton()
export class ApiV2HttpService extends ApiHttpService {
    constructor(tokenService: TokenService, messageService: MessageService) {
        super(tokenService, messageService)
    }

    apiUrl = process.env.REACT_APP_API_URL + "/v2/"
}
