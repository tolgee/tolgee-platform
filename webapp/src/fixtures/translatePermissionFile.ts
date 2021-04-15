import { useTranslate } from "@tolgee/react";
import {repositoryPermissionTypes} from "../constants/repositoryPermissionTypes";

export const translatedPermissionType = (type: string, noWrap = false) => {
    const t = useTranslate()

    return t(`permission_type_${repositoryPermissionTypes[type]}`, undefined, noWrap);
}
