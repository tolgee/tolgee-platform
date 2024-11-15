import { LanguageInfoModel, ServiceType } from './types';

export function supportsFormality(
  langInfo: LanguageInfoModel,
  serviceType: ServiceType
) {
  return langInfo.supportedServices.find((i) => i.serviceType === serviceType)
    ?.formalitySupported;
}
