const fs = require('fs');
const base = 'src/main/resources/assets/infinitycraft/models/item/';
for (const f of ['wildfire_bow', 'wildfire_bow_idle', 'wildfire_bow_pulling']) {
  JSON.parse(fs.readFileSync(base + f + '.json', 'utf8'));
  console.log(f + '.json OK');
}
