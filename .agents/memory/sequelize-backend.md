---
name: Sequelize Backend
description: Node.js + Express + Sequelize REST API that mirrors the Android Room database schema.
---

## Stack
- Runtime: Node.js 18
- Framework: Express 4
- ORM: Sequelize 6
- Database: SQLite3 (file: data/skincraft.sqlite)
- Workflow: port 5000 via `node server/index.js`

## File layout
```
server/
  index.js                   # entry point — starts Express + calls connectDB()
  config/database.js         # Sequelize instance; connectDB() syncs all models
  models/SkinProject.js      # mirrors Android Room entity (same field names/types)
  routes/skins.js            # all CRUD routes under /api/skins
  middleware/errorHandler.js # JSON error responses for Sequelize + Express errors
  middleware/requestLogger.js# morgan HTTP logging
data/
  skincraft.sqlite           # created automatically on first run
```

## API surface
| Method | Path | Description |
|--------|------|-------------|
| GET    | /health | DB connectivity check |
| GET    | /api/skins | List all (slim, no textureData); ?folder ?favorite ?search |
| GET    | /api/skins/folders | Distinct folder names |
| GET    | /api/skins/:id | Full record including textureData (base64) |
| POST   | /api/skins | Create skin; textureData must be base64 PNG |
| PUT    | /api/skins/:id | Full update |
| PATCH  | /api/skins/:id/favorite | Toggle isFavorite |
| DELETE | /api/skins/:id | Delete one |
| DELETE | /api/skins/folder/:name | Delete all skins in a folder |

## Key decisions
- textureData stored as base64 TEXT in SQLite — Android sends ByteArray as base64 over JSON
- List endpoint omits textureData for speed; use GET /:id to fetch full texture
- sequelize.sync({ alter: true }) on startup — safe column additions without data loss
- CORS open (*) in dev; set ALLOWED_ORIGINS env var to lock down in production

**Why:** Sequelize is a JS/Node ORM. The Android app uses Room for local storage;
this backend enables cloud sync / multi-device support.
