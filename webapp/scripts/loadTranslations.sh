curl "${TOLGEE_API_URL}/v2/projects/export?ak=${TOLGEE_API_KEY}&languages=en,cs,fr,es,de,pt" --output data.zip \
  && rm -rf ./src/i18n \
  && unzip data.zip -d ./src/i18n/ \
  && rm data.zip