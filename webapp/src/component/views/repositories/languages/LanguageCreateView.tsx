import * as React from 'react';
import {useEffect, useState} from 'react';
import {T} from "@tolgee/react";
import {BaseView} from "../../../layout/BaseView";
import {CreateLanguageForm} from "../../../languages/CreateLanguageForm";
import {container} from "tsyringe";
import {LanguageActions} from "../../../../store/languages/LanguageActions";
import {useRedirect} from "../../../../hooks/useRedirect";
import {LINKS, PARAMS} from "../../../../constants/links";
import {useRepository} from "../../../../hooks/useRepository";
import {LanguageDTO} from "../../../../service/response.types";

const actions = container.resolve(LanguageActions);
export const LanguageCreateView = () => {
    let createLoadable = actions.useSelector(s => s.loadables.create);
    const [cancelled, setCancelled] = useState(false);
    const repository = useRepository()

    const onSubmit = (values) => {
        const dto: LanguageDTO = {
            ...values,
        };
        actions.loadableActions.create.dispatch(repository.id, dto);
    };

    useEffect(() => {
        if (createLoadable.loaded) {
            actions.loadableReset.create.dispatch();
        }
    }, [createLoadable.loaded])

    useEffect(() => {
        if (createLoadable.loaded || cancelled) {
            setCancelled(false);
            useRedirect(LINKS.REPOSITORY_LANGUAGES, {[PARAMS.REPOSITORY_ID]: repository.id});
        }
    })

    return (
        <>
            <BaseView lg={6} md={8} xs={12} title={<T>create_language_title</T>}>
                <CreateLanguageForm loadable={createLoadable} onSubmit={onSubmit} onCancel={() => setCancelled(true)}/>
            </BaseView>
        </>
    );
};
