import { T, useTolgee } from "@tolgee/react";

export default function LanguageSelect() {
  const tolgee = useTolgee(["language"]);
  return (
    <div className="flex items-center space-x-2">
      <label
        htmlFor="language-select"
        className="block text-sm font-medium text-gray-700 mb-1"
      >
        <T keyName="label">Change Language</T>
      </label>
      <select
        value={tolgee.getLanguage()}
        onChange={(e) => tolgee.changeLanguage(e.target.value)}
        className="border border-slate-500 z-50"
      >
        <option value="en">English</option>
        <option value="es">Español</option>
        <option value="ar">العربية</option>
        <option value="zh-Hans">中文</option>
      </select>
    </div>
  );
}
