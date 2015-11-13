(function(root) {
  root.PointAssetLayer = function(params) {
    var roadLayer = params.roadLayer,
      collection = params.collection,
      map = params.map,
      roadCollection = params.roadCollection,
      selectedAsset = params.selectedAsset;

    Layer.call(this, 'pedestrianCrossing', roadLayer);
    var me = this;
    me.minZoomForContent = zoomlevels.minZoomForAssets;
    var assetLayer = new OpenLayers.Layer.Boxes('pedestrianCrossing');

    function createFeature(asset) {
      var bounds = OpenLayers.Bounds.fromArray([asset.lon, asset.lat, asset.lon + 15, asset.lat + 15]);
      var box = new OpenLayers.Marker.Box(bounds, "ffffff00", 0);
      $(box.div)
        .css('overflow', 'visible !important')
        .css('background-image', 'url(./images/center-marker.svg)');
      box.events.register('click', box, function (e) {
        selectedAsset.open(e.object.asset);
      });
      box.asset = asset;
      return box;
    }

    this.removeLayerFeatures = function() {
      assetLayer.clearMarkers();
    };

    this.refreshView = function() {
      redrawLinks(map);
      collection.fetch(map.getExtent()).then(function(assets) {
        me.removeLayerFeatures();
        _.each(assets, function(asset) {
          var box = createFeature(asset);
          assetLayer.addMarker(box);
        });
        decorateMarkers();
      });
    };

    function decorateMarkers() {
      if (selectedAsset.asset()) {
        highlightSelected();
      } else {
        unhighlightAll();
      }
    }

    function highlightSelected() {
      var partitioned = _.groupBy(assetLayer.markers, function(marker) {
        return isSelectedAsset(marker.asset);
      });
      var selected = partitioned[true];
      var unSelected = partitioned[false];
      setOpacityForMarkers(selected, '1.0');
      setOpacityForMarkers(unSelected, '0.3');
    }

    function unhighlightAll() {
      setOpacityForMarkers(assetLayer.markers, '1.0');
    }

    function setOpacityForMarkers(markers, opacity) {
      _.each(markers, function(marker) {
        $(marker.div).css('opacity', opacity);
      });
    }

    function isSelectedAsset(asset) {
      return selectedAsset.getId() === asset.id;
    }


    this.activateSelection = function() {
    };
    this.deactivateSelection = function() {
    };

    this.layerStarted = function(eventListener) {
      bindEvents(eventListener);
    };

    function bindEvents(eventListener) {
      eventListener.listenTo(eventbus, 'map:clicked', handleMapClick);
      eventListener.listenTo(eventbus, 'pedestrianCrossing:saved', me.refreshView);
      eventListener.listenTo(eventbus, 'pedestrianCrossing:selected', decorateMarkers);
      eventListener.listenTo(eventbus, 'pedestrianCrossing:unselected', decorateMarkers);
    }

    function handleMapClick(coordinates) {
      if (applicationModel.getSelectedTool() === 'Add') {
        var pixel = new OpenLayers.Pixel(coordinates.x, coordinates.y);
        createNewAsset(map.getLonLatFromPixel(pixel));
      } else if (selectedAsset.isDirty()) {
        me.displayConfirmMessage();
      } else {
        selectedAsset.close();
      }
    }

    function createNewAsset(coordinates) {
      var selectedLon = coordinates.lon;
      var selectedLat = coordinates.lat;
      var nearestLine = geometrycalculator.findNearestLine(roadCollection.getRoadsForMassTransitStops(), selectedLon, selectedLat);
      var projectionOnNearestLine = geometrycalculator.nearestPointOnLine(nearestLine, { x: selectedLon, y: selectedLat });

      var crossing = {
        lon: projectionOnNearestLine.x,
        lat: projectionOnNearestLine.y,
        mmlId: nearestLine.mmlId
      };

      selectedAsset.place(crossing);
      assetLayer.addMarker(createFeature(crossing));
      eventbus.trigger('pedestrianCrossing:opened');
    }

    function redrawLinks(map) {
      eventbus.once('roadLinks:fetched', function () {
        roadLayer.drawRoadLinks(roadCollection.getAll(), map.getZoom());
      });
      roadCollection.fetchFromVVH(map.getExtent());
    }

    function show(map) {
      redrawLinks(map);
      map.addLayer(assetLayer);
      me.show(map);
    }
    function hide() {
      map.removeLayer(assetLayer);
      roadLayer.clear();
    }
    return {
      show: show,
      hide: hide
    };
  };
})(this);