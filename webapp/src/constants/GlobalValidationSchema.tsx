import * as Yup from 'yup';
import {container} from "tsyringe";
import {SignUpService} from "../service/SignUpService";
import React from "react";
import {T} from "@tolgee/react";
import {OrganizationService} from "../service/OrganizationService";

Yup.setLocale({
    // use constant translation keys for messages without values
    mixed: {
        default: 'field_invalid',
        required: ({path}) => {
            return <T>{"Validation - required field"}</T>
        },
    },
    string: {
        email: () => <T>validation_email_is_not_valid</T>,
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

    private static readonly createEmailValidation = (): (v) => Promise<boolean> =>
        debouncedValidation((v) => {
                try {
                    Yup.string().required().email().validateSync(v)
                    return true
                } catch (e) {
                    return false
                }
            },
            container.resolve(SignUpService).validateEmail)


    static readonly SIGN_UP = (t: (key: string) => string) => Yup.object().shape({
        ...Validation.USER_PASSWORD_WITH_REPEAT_NAKED,
        name: Yup.string().required(),
        email: Yup.string().email().required()
            .test('checkEmailUnique', t('validation_email_not_unique'), Validation.createEmailValidation())
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

    static readonly ORGANIZATION_CREATE_OR_EDIT = (t: (key: string) => string, addressPartInitialValue?: string) => {

        const addressPartSyncValidation = Yup.string().required().min(3).max(60).matches(/^[a-z0-9-]*[a-z]+[a-z0-9-]*$/, {
            message: <T>address_part_validation_can_contain_just_lowercase_numbers_hyphens</T>
        });

        const addressPartUniqueDebouncedAsyncValidation = (v) => {

            if (addressPartInitialValue === v) {
                return true
            }

            return debouncedValidation(
                (v) => {
                    try {
                        addressPartSyncValidation.validateSync(v)
                        return true
                    } catch (e) {
                        return false
                    }
                },
                (v) => container.resolve(OrganizationService).validateAddressPart(v)
            )(v)
        }

        return Yup.object().shape(
            {
                name: Yup.string().required().min(3).max(50),
                addressPart: addressPartSyncValidation.test("addressPartUnique", t('validation_address_part_not_unique'), addressPartUniqueDebouncedAsyncValidation),
                description: Yup.string().nullable()
            });
    }
}

let GLOBAL_VALIDATION_DEBOUNCE_TIMER: any = undefined;

/**
 * @param syncValidationCallback sync validation callback - must return true to async validation be called
 * @param asyncValidationCallback the async validation
 * @return Promise<true> if valid
 */
const debouncedValidation = (syncValidationCallback: (v) => boolean, asyncValidationCallback: (v) => Promise<boolean>): (v) => Promise<boolean> => {
    let lastValue = undefined as any;
    let lastResult = undefined as any;
    return (v) => {
        clearTimeout(GLOBAL_VALIDATION_DEBOUNCE_TIMER);
        return new Promise((resolve) => {
            GLOBAL_VALIDATION_DEBOUNCE_TIMER = setTimeout(
                () => {
                    if (lastValue == v) {
                        resolve(lastResult);
                        return;
                    }
                    lastResult = syncValidationCallback(v) && asyncValidationCallback(v)
                    resolve(lastResult);
                    lastValue = v;
                },
                500,
            );
        });
    }
}
