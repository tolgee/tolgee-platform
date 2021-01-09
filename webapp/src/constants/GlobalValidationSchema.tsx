import * as Yup from 'yup';
import {container} from "tsyringe";
import {signUpService} from "../service/signUpService";
import React from "react";
import {T} from "@polygloat/react";

Yup.setLocale({
    // use constant translation keys for messages without values
    mixed: {
        default: 'field_invalid',
        required: ({path}) => {
            return <T>{"Validation - required field"}</T>
        },
    },
    string: {
        email: () => <T>Validation - email is not valid</T>,
        min: ({min}) => <T parameters={{min: min.toString()}}>Field should have at least n chars</T>,
        max: ({max}) => <T parameters={{max: max.toString()}}>Field should have maximum of n chars</T>,
    },
});

export class Validation {

    static readonly USER_PASSWORD = Yup.string().min(8).max(100).required();

    static readonly USER_PASSWORD_WITH_REPEAT_NAKED = {
        password: Validation.USER_PASSWORD,
        passwordRepeat: Yup.string().oneOf([Yup.ref('password'), null], 'Passwords must match').required()
    };

    static readonly USER_PASSWORD_WITH_REPEAT = Yup.object().shape(Validation.USER_PASSWORD_WITH_REPEAT_NAKED);

    static readonly RESET_PASSWORD_REQUEST = Yup.object().shape({
        email: Yup.string().email().required()
    });

    private static readonly createEmailValidation = (): (v) => Promise<boolean> => {
        let timer = null;
        const signUpServiceImpl = container.resolve(signUpService);
        let lastValue = undefined;
        let lastResult = undefined;
        return (v) => {
            clearTimeout(timer);
            return new Promise((resolve) => {
                timer = setTimeout(
                    () => {
                        if (lastValue == v) {
                            resolve(lastResult);
                            return;
                        }
                        lastResult = v && Yup.string().email().validateSync(v) && signUpServiceImpl.validateEmail(v);
                        resolve(lastResult);
                        lastValue = v;
                    },
                    500,
                );
            });
        }
    };

    static readonly SIGN_UP = Yup.object().shape({
        ...Validation.USER_PASSWORD_WITH_REPEAT_NAKED,
        name: Yup.string().required(),
        email: Yup.string().email().required()
            .test('checkEmailUnique', 'User with this e-mail already exists.', Validation.createEmailValidation())
    });

    static readonly USER_SETTINGS = Yup.object().shape({
            password: Yup.string().min(8).max(100),
            passwordRepeat: Yup.string().notRequired().oneOf([Yup.ref('password'), null], 'Passwords must match'),
            name: Yup.string().required(),
            email: Yup.string().email().required()
        }
    );


    static readonly API_KEY_SCOPES = Yup.mixed().test(
        "is-set",
        'Set at least one scope',
        v => !!(v as Set<string>).size
    );

    static readonly EDIT_API_KEY = Yup.object().shape({
        scopes: Validation.API_KEY_SCOPES
    });

    static readonly CREATE_API_KEY = Yup.object().shape({
        repositoryId: Yup.number().required(),
        scopes: Yup.mixed().test(
            "is-set",
            'Set at least one scope',
            v => !!(v as Set<string>).size
        )
    });

    static readonly TRANSLATION_KEY = Yup.string().required();

    static readonly TRANSLATION_TRANSLATION = Yup.string();

    static readonly LANGUAGE_NAME = Yup.string().required().max(100);

    static readonly LANGUAGE_ABBREVIATION = Yup.string().required().max(20);

    static readonly LANGUAGE = Yup.object().shape(
        {
            name: Validation.LANGUAGE_NAME,
            abbreviation: Validation.LANGUAGE_ABBREVIATION
        });

    static readonly KEY_TRANSLATION_CREATION = (langs: string[]) => {
        let translationValidation = langs.reduce((validation, lang) =>
            ({...validation, ["translations." + lang]: Validation.TRANSLATION_TRANSLATION}), {});
        return Yup.object().shape({key: Validation.TRANSLATION_KEY, ...translationValidation});
    };

    static readonly REPOSITORY_CREATION = Yup.object().shape(
        {
            name: Yup.string().required().min(3).max(500),
            languages: Yup.array().required().of(Yup.object().shape({
                name: Validation.LANGUAGE_NAME.label("name").required(),
                abbreviation: Validation.LANGUAGE_ABBREVIATION.label("name").required()
            }))
        });


}