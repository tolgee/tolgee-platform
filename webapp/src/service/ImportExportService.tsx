import {container, singleton} from 'tsyringe';
import {ApiV1HttpService} from './http/ApiV1HttpService';
import {MessageService} from './MessageService';
import {ImportDto} from "./request.types";
import {T} from "@tolgee/react";
import React from "react";

const http = container.resolve(ApiV1HttpService);

@singleton()
export class ImportExportService {
    constructor(private messaging: MessageService) {
    }

    readonly doImport = async (repositoryId: number, dto: ImportDto) => {
        await http.post("repository/" + repositoryId + "/import", dto);
        this.messaging.success(<T>Import - Successfully imported!</T>);
    };

    readonly exportToJsons = async (repositoryId: number) => http.getFile("repository/" + repositoryId + "/export/jsonZip");

}
