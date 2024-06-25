const decompress = require('decompress');

const unzip = ({ path, file }) =>
  decompress(path + file, path + 'unzip/' + file.replace('.zip', ''));

module.exports = {
  unzip,
};
