const { ValidationError, DatabaseError } = require('sequelize');

/**
 * Central error handler — always returns JSON so Android clients
 * can parse the response consistently.
 */
// eslint-disable-next-line no-unused-vars
function errorHandler(err, req, res, next) {
  console.error(`[ERROR] ${req.method} ${req.path}:`, err.message);

  // Sequelize field-level validation failures (e.g. bad format value)
  if (err instanceof ValidationError) {
    return res.status(422).json({
      error: 'Validation failed.',
      details: err.errors.map(e => ({ field: e.path, message: e.message })),
    });
  }

  // Sequelize DB errors (constraint violations, bad SQL, etc.)
  if (err instanceof DatabaseError) {
    return res.status(500).json({ error: 'Database error.', details: err.message });
  }

  // Express JSON parse errors
  if (err.type === 'entity.parse.failed') {
    return res.status(400).json({ error: 'Invalid JSON body.' });
  }

  const status = err.status || err.statusCode || 500;
  res.status(status).json({
    error: err.message || 'Internal server error.',
  });
}

module.exports = errorHandler;
