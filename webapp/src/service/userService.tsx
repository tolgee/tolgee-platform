import {singleton} from 'tsyringe';
import {ApiHttpService} from './apiHttpService';
import {UserDTO, UserUpdateDTO} from './response.types';
import {messageService} from './messageService';
import {T} from "@polygloat/react";
import React from "react";


@singleton()
export class userService {
    constructor(private http: ApiHttpService, private messagesService: messageService) {
    }

    public getUserData = (): Promise<UserDTO> => this.http.get("user");

    public updateUserData = async (data: UserUpdateDTO): Promise<void> => {
        await this.http.post("user", data);
        this.messagesService.success(<T>User data - Successfully updated!</T>);
    };
}