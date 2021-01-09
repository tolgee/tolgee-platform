import {singleton} from 'tsyringe';
import {ApiHttpService} from './apiHttpService';
import {ApiKeyDTO} from "./response.types";
import {messageService} from "./messageService";
import {EditApiKeyDTO} from "./request.types";
import {T} from "@polygloat/react";
import React from "react";

const BASE = "apiKeys";

@singleton()
export class apiKeysService {

    constructor(private http: ApiHttpService, private messages: messageService) {
    }

    getListForLoggedUser: () => Promise<ApiKeyDTO[]> = () =>
        this.http.get(`${BASE}`);

    getAvailableScopes: () => Promise<{ [key: string]: any[] }> = () =>
        this.http.get(`${BASE}/availableScopes`);

    generateApiKey = async (val: { repositoryId: number, scopes: string[] }): Promise<ApiKeyDTO> => {
        const res: ApiKeyDTO = await this.http.post(`${BASE}`, val);
        this.messages.success(<T>api_key_successfully_generated</T>);
        return res;
    };

    edit = async (dto: EditApiKeyDTO): Promise<void> => {
        await this.http.post(`${BASE}/edit`, dto);
        this.messages.success(<T>api_key_successfully_edited</T>);
    }

    delete = async (key: string): Promise<void> => {
        await this.http.delete(`${BASE}/${key}`);
        this.messages.success(<T>api_key_successfully_deleted</T>);
    }
}