const morgan = require('morgan');

/**
 * HTTP request logger — concise in production, verbose in dev.
 */
const format = process.env.NODE_ENV === 'production'
  ? ':remote-addr :method :url :status :res[content-length] - :response-time ms'
  : 'dev';

module.exports = morgan(format);
