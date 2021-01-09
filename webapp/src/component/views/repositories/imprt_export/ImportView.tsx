import {ChangeEvent, default as React, FunctionComponent, useEffect, useState} from 'react';
import {useRouteMatch} from 'react-router-dom';
import {PARAMS} from '../../../../constants/links';
import {Box, Button, FormHelperText, Input, LinearProgress} from '@material-ui/core';
import {BaseView} from '../../../layout/BaseView';
import {useSelector} from 'react-redux';
import {AppState} from '../../../../store';

import {StandardForm} from "../../../common/form/StandardForm";
import {TextField} from "../../../common/form/fields/TextField";
import {object, string} from "yup";
import {ImportExportActions} from "../../../../store/repository/ImportExportActions";
import {container} from "tsyringe";
import {T} from '@polygloat/react';

type SubtreeType = { [key: string]: string | object };
const actions = container.resolve(ImportExportActions);

export const ImportView: FunctionComponent = () => {
    let match = useRouteMatch();

    const repositoryId = match.params[PARAMS.REPOSITORY_ID];

    let state = useSelector((state: AppState) => state.importExport.loadables.import);


    const [data, setData] = useState(null);
    const [suggestedName, setSuggestedName] = useState("");

    const fileSelected = (event: ChangeEvent) => {
        let target = event.target as HTMLInputElement;
        if (target.files.length > 0) {
            const file = target.files[0];
            let fileReader = new FileReader();
            fileReader.onloadend = (e) => {
                const target = e.target as FileReader;
                const indexOfDot = file.name.indexOf(".");
                if (indexOfDot > -1) {
                    setSuggestedName(file.name.substr(0, indexOfDot))
                }
                const data = parseData(target.result as string);
                setData(data);
            };
            fileReader.readAsText(file);
        }
    };


    const parseSubTree = (path: string[], subtree: SubtreeType): { [key: string]: string } => {
        return Object.entries(subtree).reduce((result, [key, value]) => {
            const subPath = [...path, key];
            if (typeof value === "object") {
                return {...result, ...parseSubTree(subPath, value as SubtreeType)};
            }
            if (typeof value === "string") {
                return {...result, [subPath.join(".")]: value}
            }
            //todo handle errors here!
            return result;
        }, {});
    };

    const parseData = (json: string) => {
        const data = JSON.parse(json);
        return parseSubTree([], data);
    };


    const entries = data && Object.entries(data);

    const Line = ([key, translation]) => <Box>{key}: {translation}</Box>;

    const Preview = () => {

        const [expanded, setExpanded] = useState(false);

        const expand = () => {
            setExpanded(true);
        };

        return (
            <Box color="text.disabled">
                {expanded ? <> {entries.map(Line)} </>
                    :
                    <>
                        {entries.slice(0, 10).map(Line)}
                        {entries.length > 10 &&
                        <>
                            <Box justifyItems="center"><Button onClick={() => expand()}>...</Button></Box>
                            {
                                //render last item
                                entries.slice(entries.length - 1).map(Line)}
                        </>}
                    </>}
            </Box>
        )
    };

    useEffect(() => {
        if (state.loaded) {
            actions.loadableReset.import.dispatch();
        }
    }, [state.loaded]);

    const onImportSubmit = (value) => {
        actions.loadableActions.import.dispatch(repositoryId, {...value, data});
    };

    return (
        <BaseView title={<T>import_translations_title</T>} xs={12} md={10} lg={8}>
            <Box mt={2}>
                {
                    (data &&
                        <>
                            <Preview/>
                            <Box color="success.main" fontSize={21} fontWeight="400" mt={1}>
                                <T parameters={{length: entries.length + ""}}>import_translations_loaded_message</T>
                            </Box>
                            {!state.loaded &&
                            <>
                                <StandardForm initialValues={{languageAbbreviation: suggestedName}}
                                              validationSchema={object().shape({
                                                  languageAbbreviation: string().required()
                                              })}
                                              onSubmit={onImportSubmit}
                                              onCancel={() => setData(null)}
                                              loading={state.loading}
                                              submitButtonInner={<T>import_do_import_button</T>}>
                                    <TextField label={<T>import_language_abbreviation</T>} name={"languageAbbreviation"}/>
                                </StandardForm>
                                {state.loading &&
                                <>
                                    <Box justifyContent="center" display="flex" fontSize={20} color="text.secondary"><T>import_importing_text</T></Box>
                                    <LinearProgress/>
                                </>}
                            </>
                            }
                        </>
                    )
                    ||
                    <>
                        <FormHelperText><T>import_select_file</T></FormHelperText>
                        <Input type="file" onChange={fileSelected}/>
                    </>
                }
            </Box>
        </BaseView>
    );
};
