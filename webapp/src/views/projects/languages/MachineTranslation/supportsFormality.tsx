import { LanguageInfoModel, ServiceType } from './types';

export function supportsFormality(
  info: LanguageInfoModel[],
  serviceType: ServiceType,
  languageId: number | undefined | null
) {
  if (!languageId) {
    return info.find((l) =>
      l.supportedServices.find(
        (s) => s.serviceType === serviceType && s.formalitySupported
      )
    );
  }

  const settings = info?.find((l) => l.languageId === languageId);
  return settings?.supportedServices.find((i) => i.serviceType === serviceType)
    ?.formalitySupported;
}
