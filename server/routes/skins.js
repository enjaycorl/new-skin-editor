const express = require('express');
const { Op } = require('sequelize');
const SkinProject = require('../models/SkinProject');

const router = express.Router();

// ─────────────────────────────────────────────────────────────────────────────
// GET /api/skins
//   ?folder=My Skins   — filter by folder
//   ?favorite=true      — only favourites
//   ?search=name        — name contains (case-insensitive)
//   ?sort=lastUpdated   — sort field (default: lastUpdated DESC)
// ─────────────────────────────────────────────────────────────────────────────
router.get('/', async (req, res, next) => {
  try {
    const { folder, favorite, search, sort = 'lastUpdated' } = req.query;
    const where = {};

    if (folder)    where.folder     = folder;
    if (favorite === 'true') where.isFavorite = true;
    if (search)    where.name       = { [Op.like]: `%${search}%` };

    const allowed = ['lastUpdated', 'name', 'id'];
    const order   = [[ allowed.includes(sort) ? sort : 'lastUpdated', 'DESC' ]];

    const projects = await SkinProject.findAll({ where, order });
    // Strip heavy textureData from list response for speed
    const slim = projects.map(p => {
      const { textureData, ...rest } = p.toJSON();
      return { ...rest, hasTexture: !!textureData };
    });
    res.json({ count: slim.length, data: slim });
  } catch (err) { next(err); }
});

// ─────────────────────────────────────────────────────────────────────────────
// GET /api/skins/folders  — distinct folder names
// ─────────────────────────────────────────────────────────────────────────────
router.get('/folders', async (req, res, next) => {
  try {
    const rows = await SkinProject.findAll({
      attributes: ['folder'],
      group: ['folder'],
      order: [['folder', 'ASC']],
    });
    res.json({ folders: rows.map(r => r.folder) });
  } catch (err) { next(err); }
});

// ─────────────────────────────────────────────────────────────────────────────
// GET /api/skins/:id  — full record including textureData
// ─────────────────────────────────────────────────────────────────────────────
router.get('/:id', async (req, res, next) => {
  try {
    const skin = await SkinProject.findByPk(req.params.id);
    if (!skin) return res.status(404).json({ error: 'Skin not found.' });
    res.json({ data: skin });
  } catch (err) { next(err); }
});

// ─────────────────────────────────────────────────────────────────────────────
// POST /api/skins  — create a new skin
// Body (JSON):
//   { name, format, modelType, textureData (base64), folder?, isFavorite?, lastUpdated? }
// ─────────────────────────────────────────────────────────────────────────────
router.post('/', async (req, res, next) => {
  try {
    const { name, format, modelType, textureData, folder, isFavorite, lastUpdated } = req.body;

    if (!name)        return res.status(400).json({ error: 'name is required.' });
    if (!textureData) return res.status(400).json({ error: 'textureData (base64) is required.' });

    const skin = await SkinProject.create({
      name,
      format:      format      || '64x64',
      modelType:   modelType   || 'STEVE',
      textureData,
      folder:      folder      || 'My Skins',
      isFavorite:  isFavorite  ?? false,
      lastUpdated: lastUpdated ?? Date.now(),
    });

    res.status(201).json({ data: skin, message: 'Skin created.' });
  } catch (err) { next(err); }
});

// ─────────────────────────────────────────────────────────────────────────────
// PUT /api/skins/:id  — full update (upsert-style)
// ─────────────────────────────────────────────────────────────────────────────
router.put('/:id', async (req, res, next) => {
  try {
    const skin = await SkinProject.findByPk(req.params.id);
    if (!skin) return res.status(404).json({ error: 'Skin not found.' });

    const { name, format, modelType, textureData, folder, isFavorite } = req.body;

    await skin.update({
      ...(name        !== undefined && { name }),
      ...(format      !== undefined && { format }),
      ...(modelType   !== undefined && { modelType }),
      ...(textureData !== undefined && { textureData }),
      ...(folder      !== undefined && { folder }),
      ...(isFavorite  !== undefined && { isFavorite }),
      lastUpdated: Date.now(),
    });

    res.json({ data: skin, message: 'Skin updated.' });
  } catch (err) { next(err); }
});

// ─────────────────────────────────────────────────────────────────────────────
// PATCH /api/skins/:id/favorite  — toggle favourite flag
// ─────────────────────────────────────────────────────────────────────────────
router.patch('/:id/favorite', async (req, res, next) => {
  try {
    const skin = await SkinProject.findByPk(req.params.id);
    if (!skin) return res.status(404).json({ error: 'Skin not found.' });

    await skin.update({ isFavorite: !skin.isFavorite, lastUpdated: Date.now() });
    res.json({ data: skin, message: `isFavorite set to ${skin.isFavorite}.` });
  } catch (err) { next(err); }
});

// ─────────────────────────────────────────────────────────────────────────────
// DELETE /api/skins/:id
// ─────────────────────────────────────────────────────────────────────────────
router.delete('/:id', async (req, res, next) => {
  try {
    const skin = await SkinProject.findByPk(req.params.id);
    if (!skin) return res.status(404).json({ error: 'Skin not found.' });

    await skin.destroy();
    res.json({ message: `Skin #${req.params.id} deleted.` });
  } catch (err) { next(err); }
});

// ─────────────────────────────────────────────────────────────────────────────
// DELETE /api/skins/folder/:name  — delete all skins in a folder
// ─────────────────────────────────────────────────────────────────────────────
router.delete('/folder/:name', async (req, res, next) => {
  try {
    const count = await SkinProject.destroy({ where: { folder: req.params.name } });
    res.json({ message: `Deleted ${count} skin(s) from folder "${req.params.name}".` });
  } catch (err) { next(err); }
});

module.exports = router;
