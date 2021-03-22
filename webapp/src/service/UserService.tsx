import {singleton} from 'tsyringe';
import {ApiHttpService} from './apiHttpService';
import {UserDTO, UserUpdateDTO} from './response.types';
import {MessageService} from './MessageService';
import {T} from "@tolgee/react";
import React from "react";


@singleton()
export class UserService {
    constructor(private http: ApiHttpService, private messagesService: MessageService) {
    }

    public getUserData = (): Promise<UserDTO> => this.http.get("user");

    public updateUserData = async (data: UserUpdateDTO): Promise<void> => {
        await this.http.post("user", data);
        this.messagesService.success(<T>User data - Successfully updated!</T>);
    };
}