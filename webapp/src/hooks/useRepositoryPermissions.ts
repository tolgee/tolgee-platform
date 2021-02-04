import {useRepository} from "./useRepository";
import {RepositoryPermissionType} from "../service/response.types";

export class RepositoryPermissions {
    constructor(private activePermission: RepositoryPermissionType) {
    }

    private static readonly ORDERED_PERMISSIONS = [
        RepositoryPermissionType.VIEW,
        RepositoryPermissionType.TRANSLATE,
        RepositoryPermissionType.EDIT,
        RepositoryPermissionType.MANAGE
    ]

    satisfiesPermission(type: RepositoryPermissionType) {
        const requiredPower = RepositoryPermissions.ORDERED_PERMISSIONS.findIndex(p => p === type);
        const activePower = RepositoryPermissions.ORDERED_PERMISSIONS.findIndex(p => p === this.activePermission);
        return requiredPower <= activePower;
    }
}

export const useRepositoryPermissions = (): RepositoryPermissions => {
    const repository = useRepository();

    return new RepositoryPermissions(repository.permissionType)
};
