const { Sequelize } = require('sequelize');
const path = require('path');

const DB_PATH = process.env.DB_PATH || path.join(__dirname, '../../data/skincraft.sqlite');

const sequelize = new Sequelize({
  dialect: 'sqlite',
  storage: DB_PATH,
  logging: process.env.NODE_ENV === 'development'
    ? (msg) => console.log(`[SQL] ${msg}`)
    : false,
  define: {
    // Map camelCase JS fields → snake_case SQLite columns automatically
    underscored: false,
    timestamps: false, // we manage lastUpdated ourselves
  },
});

/**
 * Test the connection and sync all models to the database.
 * Call once at startup.
 */
async function connectDB() {
  await sequelize.authenticate();
  console.log('[DB] SQLite connection established.');
  // alter: true safely adds new columns without dropping existing data
  await sequelize.sync({ alter: true });
  console.log('[DB] All models synced to database.');
}

module.exports = { sequelize, connectDB };
