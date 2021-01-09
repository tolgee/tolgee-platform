import {container, singleton} from 'tsyringe';

import {repositoryService} from '../../service/repositoryService';
import {RepositoryDTO} from '../../service/response.types';
import {LINKS} from "../../constants/links";
import {AbstractLoadableActions, createLoadable, Loadable, StateWithLoadables} from "../AbstractLoadableActions";
import React from "react";
import {T} from "@polygloat/react";

export class RepositoriesState extends StateWithLoadables<RepositoryActions> {
    repositoriesLoading: boolean = true;
    repositories: RepositoryDTO[];
}

@singleton()
export class RepositoryActions extends AbstractLoadableActions<RepositoriesState> {
    constructor() {
        super(new RepositoriesState());
    }

    private service = container.resolve(repositoryService);

    public loadRepositories = this.createPromiseAction<RepositoryDTO[], any>('LOAD_ALL', this.service.getRepositories)
        .build.onFullFilled((state, action) => {
            return {...state, repositories: action.payload, repositoriesLoading: false};
        }).build.onPending((state) => {
            return {...state, repositoriesLoading: true};
        });


    loadableDefinitions = {
        editRepository: this.createLoadableDefinition((id, values) => this.service.editRepository(id, values), null,
            <T>repository_successfully_edited_message</T>, LINKS.REPOSITORIES),
        createRepository: this.createLoadableDefinition((values) => this.service.createRepository(values),
            null, <T>repository_created_message</T>, LINKS.REPOSITORIES),
        repository: this.createLoadableDefinition(this.service.loadRepository),
        deleteRepository: this.createLoadableDefinition(this.service.deleteRepository, (state): RepositoriesState =>
            (
                {...state, loadables: {...state.loadables, repository: {...createLoadable()} as Loadable<RepositoryDTO>}}
            ), <T>repository_deleted_message</T>)
    };


    get prefix(): string {
        return 'REPOSITORIES';
    }

}
