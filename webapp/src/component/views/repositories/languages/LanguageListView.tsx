import * as React from 'react';
import {useEffect} from 'react';
import {ListItem} from '@material-ui/core';
import Box from '@material-ui/core/Box';
import {container} from 'tsyringe';
import {LINKS, PARAMS} from '../../../../constants/links';
import {FabAddButtonLink} from '../../../common/buttons/FabAddButtonLink';
import List from '@material-ui/core/List';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import ListItemText from '@material-ui/core/ListItemText';
import {SettingsIconButton} from '../../../common/buttons/SettingsIconButton';
import {Link, useRouteMatch} from 'react-router-dom';
import {LanguageActions} from '../../../../store/languages/LanguageActions';
import {BaseView} from '../../../layout/BaseView';
import {T} from "@polygloat/react";

const actions = container.resolve(LanguageActions);

export const LanguageListView = () => {
    const match = useRouteMatch();
    const repositoryId = match.params[PARAMS.REPOSITORY_ID];

    let loadable = actions.useSelector(s => s.loadables.list);

    useEffect(() => {
        actions.loadableActions.list.dispatch(repositoryId);
    }, []);

    return (
        <BaseView title={<T>languages_title</T>} loading={loadable.loading || !loadable.touched} lg={5} md={7}>
            {() => (
                <Box ml={-2}>
                    <List>
                        {loadable.data.map(l => (
                            <ListItem key={l.id}>
                                <ListItemText>
                                    {l.name} [{l.abbreviation}]
                                </ListItemText>
                                <ListItemSecondaryAction>
                                    <Link to={LINKS.REPOSITORY_LANGUAGE_EDIT.build(
                                            {
                                                [PARAMS.REPOSITORY_ID]: repositoryId,
                                                [PARAMS.LANGUAGE_ID]: l.id
                                            }
                                        )}>
                                            <SettingsIconButton/>
                                        </Link>
                                    </ListItemSecondaryAction>
                                </ListItem>
                            ))}
                        </List>
                        <Box display="flex" flexDirection="column" alignItems="flex-end" pr={2} mt={5}>
                            <FabAddButtonLink to={LINKS.REPOSITORY_LANGUAGES_CREATE.build({[PARAMS.REPOSITORY_ID]: repositoryId})}/>
                        </Box>
                    </Box>
                )}
            </BaseView>
    );
};
