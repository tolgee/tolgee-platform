import {container, singleton} from 'tsyringe';
import {ApiV1HttpService} from './http/ApiV1HttpService';
import {MessageService} from './MessageService';
import React from "react";
import {ApiSchemaHttpService} from "./http/ApiSchemaHttpService";
import {components} from "./apiSchema";

const http = container.resolve(ApiV1HttpService);
const schemaHttpService = container.resolve(ApiSchemaHttpService);

@singleton()
export class ImportExportService {
    constructor(private messaging: MessageService) {
    }

    readonly addFiles = async (repositoryId: number, files: File[]) => schemaHttpService
        .schemaRequestRaw("/v2/repositories/{repositoryId}/import/with-streaming-response", "post")(
            {
                path: {
                    repositoryId
                },
                content: {
                    "multipart/form-data": {
                        files: files as any
                    }
                }
            }
        );

    readonly loadData = async (repositoryId: number, pageable: components["schemas"]["Pageable"]) => schemaHttpService
        .schemaRequest("/v2/repositories/{repositoryId}/import/result", "get",{
            disableNotFoundHandling: true
        })(
            {
                path: {
                    repositoryId
                },
                query: {
                    pageable: pageable
                }
            }
        );

    readonly exportToJsons = async (repositoryId: number) => http.getFile("repository/" + repositoryId + "/export/jsonZip");
}
