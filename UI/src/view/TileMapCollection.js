(function(root) {
  root.TileMapCollection = function(map, arcgisConfig) {
    var layerConfig = {
      // minResolution: ?,
      // maxResolution: ?,
      visible: false,
      extent: [-548576, 6291456, 1548576, 8388608]
    };

    var sourceConfig = {
      cacheSize: 4096,
      projection: 'EPSG:3067',
      tileSize: [256,256]
    };

    var tileGridConfig = {
      extent: [-548576, 6291456, 1548576, 8388608],
      origin: [-548576, 8388608],
      projection: 'EPSG:3067',
      // maxZoom: 22,
      resolutions: [8192, 4096, 2048, 1024, 512, 256, 128, 64, 32, 16, 8, 4, 2, 1, 0.5]
    };

    var aerialMapConfig = _.merge({}, sourceConfig, {
      url: 'maasto/wmts/1.0.0/ortokuva/default/ETRS-TM35FIN/{z}/{y}/{x}.jpg'
    });

    var backgroundMapConfig = _.merge({}, sourceConfig, {
      url: 'maasto/wmts/1.0.0/taustakartta/default/ETRS-TM35FIN/{z}/{y}/{x}.png'
    });

    var terrainMapConfig = _.merge({}, sourceConfig, {
      url: 'maasto/wmts/1.0.0/maastokartta/default/ETRS-TM35FIN/{z}/{y}/{x}.png'
    });

    var parser = new ol.format.WMTSCapabilities();
    var result = parser.read(arcgisConfig);
    var config = {layer: "Taustakartat_Harmaasavy"};
    var options = ol.source.WMTS.optionsFromCapabilities(result, config);
    var greyscaleLayer = new ol.layer.Tile({source: new ol.source.WMTS(options)});

    var aerialMapLayer = new ol.layer.Tile(_.merge({
      source: new ol.source.XYZ(_.merge({
        tileGrid: new ol.tilegrid.TileGrid(tileGridConfig)
      }, aerialMapConfig))
    }, layerConfig));

    var backgroundMapLayer = new ol.layer.Tile(_.merge({
      source: new ol.source.XYZ(_.merge({
        tileGrid: new ol.tilegrid.TileGrid(tileGridConfig)
      }, backgroundMapConfig))
    }, layerConfig));

    var terrainMapLayer = new ol.layer.Tile(_.merge({
      source: new ol.source.XYZ(_.merge({
        tileGrid: new ol.tilegrid.TileGrid(tileGridConfig)
      }, terrainMapConfig))
    }, layerConfig));
    var tileMapLayers = {
      background: backgroundMapLayer,
      greyscale: greyscaleLayer,
      aerial: aerialMapLayer,
      terrain: terrainMapLayer
    };

    var selectMap = function(tileMap) {
      _.forEach(tileMapLayers, function(layer, key) {
        if (key === tileMap) {
          layer.setVisible(true);
        } else {
          layer.setVisible(false);
        }
      });
    };

    selectMap('background');
    eventbus.on('tileMap:selected', selectMap);

    return {
      layers: [backgroundMapLayer, aerialMapLayer, terrainMapLayer]
    };
  };
})(this);