import {useSelector} from "react-redux";
import {AppState} from "../store";
import {container} from "tsyringe";
import {GlobalActions} from "../store/global/GlobalActions";


const globalActions = container.resolve(GlobalActions)
export const useLoading = () => {
    return useSelector((state: AppState) => state.global.loading)
}

export const startLoading = () => {
    globalActions.startLoading.dispatch()
}

export const stopLoading = () => {
    globalActions.stopLoading.dispatch()
}
