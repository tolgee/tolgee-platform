import {singleton} from 'tsyringe';
import {AbstractLoadableActions, StateWithLoadables} from "../AbstractLoadableActions";
import {AppState} from "../index";
import {useSelector} from "react-redux";
import {ScreenshotService} from "../../service/ScreenshotService";
import {ScreenshotDTO} from "../../service/response.types";

export class ScreenshotState extends StateWithLoadables<ScreenshotActions> {

}

@singleton()
export class ScreenshotActions extends AbstractLoadableActions<ScreenshotState> {
    constructor(private service: ScreenshotService) {
        super(new ScreenshotState());
    }

    readonly loadableDefinitions = {
        getForKey: this.createLoadableDefinition((repositoryId, key) => this.service.getForKey(repositoryId, key)),
        uploadScreenshot: this.createLoadableDefinition((...args: Parameters<ScreenshotService["upload"]>) => this.service.upload(...args),
            (state, action) => {
                const s = state as any; //workaround circular type reference
                return {
                    ...s, loadables: {
                        ...s.loadables, getForKey: {
                            ...s.loadables.getForKey,
                            data: [...s.loadables.getForKey.data as ScreenshotDTO[], ...action.payload.stored]
                        }
                    } as any
                } as ScreenshotState
            }),
        delete: this.createDeleteDefinition("getForKey", (id) => this.service.delete(id)
        )
    };

    useSelector<T>(selector: (state: ScreenshotState) => T): T {
        return useSelector((state: AppState) => selector(state.screenshots))
    }

    get prefix(): string {
        return 'SCREENSHOTS';
    }
}