const fs = require('fs');
const path = require('path');
const fetch = (...args) =>
  import('node-fetch').then(({ default: fetch }) => fetch(...args));

const host = process.env.HOST ?? 'http://localhost:8080';

(async () => {
  const scopes = await getScopes();
  const content = JSON.stringify(scopes, null, 2);
  fs.writeFileSync(
    path.resolve(__dirname + '/../src/constants/scopes.generated.json'),
    content
  );
})();

async function getScopes() {
  const data = await fetch(`${host}/v2/public/scope-info/hierarchy`);
  return data.json();
}
