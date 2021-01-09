import {container, singleton} from 'tsyringe';
import {ApiHttpService} from './apiHttpService';
import {messageService} from './messageService';
import {ImportDto} from "./request.types";
import {T} from "@polygloat/react";
import React from "react";

const http = container.resolve(ApiHttpService);

@singleton()
export class importExportService {
    constructor(private messaging: messageService) {
    }

    readonly doImport = async (repositoryId: number, dto: ImportDto) => {
        await http.post("repository/" + repositoryId + "/import", dto);
        this.messaging.success(<T>Import - Successfully imported!</T>);
    };

    readonly exportToJsons = async (repositoryId: number) => http.getFile("repository/" + repositoryId + "/export/jsonZip");

}
