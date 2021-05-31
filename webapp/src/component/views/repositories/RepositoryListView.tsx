import * as React from 'react';
import Box from '@material-ui/core/Box';
import {container} from 'tsyringe';
import {RepositoryActions} from '../../../store/repository/RepositoryActions';
import {LINKS} from '../../../constants/links';
import {FabAddButtonLink} from '../../common/buttons/FabAddButtonLink';
import {BaseView} from '../../layout/BaseView';
import {PossibleRepositoryPage} from "../PossibleRepositoryPage";
import {useTranslate} from "@tolgee/react";
import {SimplePaginatedHateoasList} from "../../common/list/SimplePaginatedHateoasList";
import RepositoryListItem from "./RepositoryListItem";

const actions = container.resolve(RepositoryActions);

export const RepositoryListView = () => {

    const listPermitted = actions.useSelector(state => state.loadables.listPermitted)

    const t = useTranslate();

    return (
        <PossibleRepositoryPage>
            <BaseView title={t("repositories_title")} containerMaxWidth="md" hideChildrenOnLoading={false} loading={listPermitted.loading}>
                <SimplePaginatedHateoasList searchField pageSize={20} actions={actions} loadableName="listPermitted" renderItem={r =>
                    <RepositoryListItem key={r.id} {...r} />
                }/>
                <Box display="flex" flexDirection="column" alignItems="flex-end" mt={2} pr={2}>
                    <FabAddButtonLink to={LINKS.REPOSITORY_ADD.build()}/>
                </Box>
            </BaseView>
        </PossibleRepositoryPage>
    );
};

