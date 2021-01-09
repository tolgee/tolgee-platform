import {API_URL, PASSWORD, USERNAME} from "./constants";
import {LanguageDTO} from "../../src/service/response.types";
import {ArgumentTypes, Scope} from "./types";

const bcrypt = require('bcryptjs');

let token = null;

const apiFetch = (input: string, init?: ArgumentTypes<typeof cy.request>[0], headers = {}) => {
    return cy.request({
        url: API_URL + input,
        headers: {
            'Content-Type': 'application/json',
            'Authorization': "Bearer " + token,
            ...headers
        },
        ...init
    })
}

const internalFetch = (input: string, init?: ArgumentTypes<typeof cy.request>[0]) => {
    return cy.request({
        url: API_URL.replace("/api/", "/internal/") + input,
        headers: {
            'Content-Type': 'application/json',
            'Authorization': "Bearer " + token
        },
        ...init
    })
}


export const login = (username = USERNAME, password = PASSWORD) => {
    return cy.request({
        url: API_URL + "public/generatetoken",
        method: "POST",
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            username,
            password
        })
    }).then(res => {
        token = res.body.accessToken;
        window.localStorage.setItem('jwtToken', token);
    })
}

export const createRepository = (createRepositoryDto: { name: string, languages: Partial<LanguageDTO>[] }) => {
    const create = () => apiFetch("repositories", {body: JSON.stringify(createRepositoryDto), method: "POST"});
    return apiFetch("repositories").then(res => {
        const test = res.body.find(i => i.name === createRepositoryDto.name)
        if (test) {
            return deleteRepository(test.id).then(() => create());
        }

        return create();
    })
}

export const createTestRepository = () => createRepository({
    name: "Test", languages: [{abbreviation: "en", name: "English"}]
});

export const setTranslations = (repositoryId, key: string, translations: { [lang: string]: string }) =>
    apiFetch(`repository/${repositoryId}/keys/create`, {body: {key, translations}, method: "POST"});

export const deleteRepository = (id: number) => {
    return apiFetch(`repositories/${id}`, {method: "DELETE"});
}

export const createUser = (username: string = "test", password: string = "test", fullName = "Test Full Name") => {
    password = bcrypt.hashSync(password, bcrypt.genSaltSync(10));

    return deleteUser(username).then(() => {
        const sql = `insert into user_account (username, name, password, created_at, updated_at)
            values ('${username}', '${fullName}', '${password}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)`;
        return internalFetch(`sql/execute`, {method: "POST", body: sql});
    });
}

export const deleteUser = (username: string) => {
    const deleteUserSql = `delete from user_account where username='${username}'`;
    return internalFetch(`sql/execute`, {method: "POST", body: deleteUserSql})
}

export const deleteUserWithInvitationCode = (username: string) => {
    const sql = `
        delete from email_verification where user_account_id in (select id from user_account where username='${username}');
        delete from user_account where username='${username}';
    `;

    return internalFetch(`sql/execute`, {method: "POST", body: sql})
}

export const getUser = (username: string) => {
    const sql = `select user_account.username, email_verification.id from user_account 
    join email_verification on email_verification.user_account_id = user_account.id
    where username='${username}'`;
    return internalFetch(`sql/list`, {method: "POST", body: sql}).then((r) => {
        return r.body[0];
    })
}

export const createApiKey = (body: { repositoryId: number, scopes: Scope[] }) => apiFetch(`apiKeys`, {method: "POST", body}).then(r => r.body)

export const addScreenshot = (repositoryId: number, key: string, path: string) => {
    return cy.fixture(path).then(f => {
        const blob = Cypress.Blob.base64StringToBlob(f, 'image/png')
        const data = new FormData();
        data.append("screenshot", blob);
        data.append("key", key);
        cy.log("Uploading screenshot: " + path);
        return fetch(`${API_URL}repository/${repositoryId}/screenshots`, {
            headers: {
                'Authorization': "Bearer " + token,
            },
            method: "POST", body: data
        }).then((r) => {
            if (r.status > 200) {
                r.text().then(t => console.error(t));
                throw new Error("Error response from server");
            }
            cy.log("Image uploaded");
        });
    })
}
