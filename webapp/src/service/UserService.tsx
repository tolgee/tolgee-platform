import {singleton} from 'tsyringe';
import {ApiV1HttpService} from './http/ApiV1HttpService';
import {UserDTO, UserUpdateDTO} from './response.types';
import {MessageService} from './MessageService';
import {T} from "@tolgee/react";
import React from "react";
import {components} from "./apiSchema";


@singleton()
export class UserService {
    constructor(private http: ApiV1HttpService, private messagesService: MessageService) {
    }

    public getUserData = (): Promise<components["schemas"]["UserResponseDTO"]> => this.http.get("user");

    public updateUserData = async (data: UserUpdateDTO): Promise<void> => {
        await this.http.post("user", data);
        this.messagesService.success(<T>User data - Successfully updated!</T>);
    };
}
