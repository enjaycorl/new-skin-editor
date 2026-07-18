const { DataTypes } = require('sequelize');
const { sequelize } = require('../config/database');

/**
 * SkinProject — mirrors the Android Room entity exactly:
 *
 *   id          INT  PK autoincrement
 *   name        TEXT
 *   format      TEXT  ("64x32" | "64x64" | "128x128")
 *   modelType   TEXT  ("STEVE" | "ALEX")
 *   textureData BLOB  (raw PNG bytes, base64 over JSON)
 *   folder      TEXT
 *   isFavorite  BOOLEAN
 *   lastUpdated BIGINT (epoch ms, matches System.currentTimeMillis())
 */
const SkinProject = sequelize.define('SkinProject', {
  id: {
    type: DataTypes.INTEGER,
    primaryKey: true,
    autoIncrement: true,
  },
  name: {
    type: DataTypes.STRING,
    allowNull: false,
    validate: { notEmpty: { msg: 'Skin name cannot be empty.' } },
  },
  format: {
    type: DataTypes.STRING,
    allowNull: false,
    defaultValue: '64x64',
    validate: {
      isIn: {
        args: [['64x32', '64x64', '128x128']],
        msg: 'format must be one of: 64x32, 64x64, 128x128',
      },
    },
  },
  modelType: {
    type: DataTypes.STRING,
    allowNull: false,
    defaultValue: 'STEVE',
    validate: {
      isIn: {
        args: [['STEVE', 'ALEX']],
        msg: 'modelType must be STEVE or ALEX',
      },
    },
  },
  // Stored as base64 string in SQLite; Android sends/receives base64
  textureData: {
    type: DataTypes.TEXT('long'),
    allowNull: false,
    comment: 'Base64-encoded PNG texture sheet bytes',
  },
  folder: {
    type: DataTypes.STRING,
    allowNull: false,
    defaultValue: 'My Skins',
  },
  isFavorite: {
    type: DataTypes.BOOLEAN,
    allowNull: false,
    defaultValue: false,
  },
  lastUpdated: {
    type: DataTypes.BIGINT,
    allowNull: false,
    defaultValue: () => Date.now(),
  },
}, {
  tableName: 'skin_projects',
  timestamps: false,
});

module.exports = SkinProject;
