(function(root) {

    root.LinearAssetLabel = function(){
        AssetLabel.call(this);
        var me = this;

        var backgroundStyle = function (value) {

          var valueLength = value.toString().length;
          var image = 'images/linearLabel_background.png';

          if (!me.isValidValue(value)) {
            image = 'images/warningLabel.png';
          }else if (valueLength > 4 && valueLength < 7) {
            image = 'images/linearLabel_background_large.png';
          }

          return new ol.style.Style({
            image: new ol.style.Icon(({
              src: image
            }))
          });
        };

        var textStyle = function(value) {
          if (!me.isValidValue(value))
            return '';
          return "" + value;
        };

        this.isValidValue = function(value){
          var valueLength = value.toString().length;
          if(value)
            if(valueLength > 6 || value < 0)
              return false;
            return true;
        };

        this.getStyle = function(value){
          return [backgroundStyle(value), new ol.style.Style({
            text : new ol.style.Text({
              text : textStyle(value),
              fill: new ol.style.Fill({
                  color: '#ffffff'
              }),
              font : '12px sans-serif'
            })
          })];
        };

        this.getValue = function(asset){
            return asset.value;
        };

    };

    root.SpeedLimitAssetLabel = function() {
        LinearAssetLabel.call(this);

        this.isValidValue = function(value) {
            return value && value > 0 && value <= 120;
        };
    };

    root.LinearAssetLabelMultiValues = function(){

        AssetLabel.call(this);

        var me = this;
        var IMAGE_HEIGHT = 27;
        var IMAGE_ADJUSTMENT = 15;

        this.getStyle = function(value){
          return createMultiStyles(value);
        };

        var createMultiStyles = function(values){
          var i = 0;
          var splitValues = values.replace(/[ \t\f\v]/g,'').split(/[\n,]+/);
          var styles = [];
          _.forEach(splitValues, function(value){
            i++;
            styles.push(backgroundStyle(value, i), textStyle(value, i));
          });
          return styles;
        };

        var backgroundStyle = function(value, i){
          var image = 'images/linearLabel_background.png';
          if(!correctValues(value))
            image = 'images/warningLabel.png';

          return new ol.style.Style({
            image: new ol.style.Icon(({
              anchor: [IMAGE_ADJUSTMENT+2, (i * IMAGE_HEIGHT) - IMAGE_ADJUSTMENT],
              anchorXUnits: 'pixels',
              anchorYUnits: 'pixels',
              src: image
            }))
          });
        };

        var textStyle = function(value, i) {

          return new ol.style.Style({
            text: new ol.style.Text(({
              text: getTextValue(value),
              offsetX: 0,
              offsetY: (-i*IMAGE_HEIGHT)+IMAGE_HEIGHT,
              textAlign: 'center',
              fill: new ol.style.Fill({
                color: '#ffffff'
              }),
              font : '12px sans-serif'
            }))
          });
        };

        var getTextValue = function(value) {
          if(!correctValues(value))
            return '';
          return '' + value;
        };

        var correctValues = function(value){
          var valueLength = value.toString().length;
          if(value){
            return value.match(/^[0-9|Ee]/) && valueLength < 4;
          }
          return true;
        };

        this.getValue = function(asset){
            return asset.value;
        };

    };

    root.MassLimitationsLabel = function () {

      AssetLabel.call(this);
      var me = this;

      var backgroundStyle = function (value, counter) {
        return new ol.style.Style({
          image: new ol.style.Icon(({
            src: getImage(value),
            anchor : [0.5, 1 + counter]
          }))
        });
      };

      var textStyle = function (value) {
        if (!me.isValidValue(value))
          return '';
        // conversion Kg -> t
        return ''.concat(value/1000, 't');
      };

      this.isValidValue = function (value) {
        return value;
      };

      this.getStyle = function (asset, count) {
        return [backgroundStyle(getTypeId(asset), count), new ol.style.Style({
          text: new ol.style.Text({
            text: textStyle(me.getValue(asset)),
            fill: new ol.style.Fill({
              color: '#000000'
            }),
            font: '14px sans-serif',
            offsetY: getTextOffset(asset, count)
          })
        })];
      };

      var getImage = function (typeId) {
        var images = {
          30: 'images/mass-limitations/totalWeightLimit.png'   ,
          40: 'images/mass-limitations/trailerTruckWeightLimit.png',
          50: 'images/mass-limitations/axleWeightLimit.png',
          60: 'images/mass-limitations/bogieWeightLimit.png'
        };
        return images[typeId];
      };


      var getTextOffset = function (asset, count) {
        var offsets = { 30: -17 - (count * 35), 40: -12 - (count * 35), 50: -20 - (count * 35), 60: -20 - (count * 35)};
        return offsets[getTypeId(asset)];
      };

      var getValues = function (asset) {
        return asset.values;
      };

      this.getValue = function (asset) {
        return asset.value;
      };

      var getTypeId = function (asset) {
        return asset.typeId;
      };

      this.renderFeatures = function (assets, zoomLevel, getPoint) {
        if (!me.isVisibleZoom(zoomLevel))
          return [];


        return [].concat.apply([], _.chain(assets).map(function (asset) {
          var values = getValues(asset);
          var count = 0;
          return _.map(values, function (assetValues) {
            var style = me.getStyle(assetValues, count++, getPoint(asset)[1]);
            var feature = me.createFeature(getPoint(asset));
            feature.setStyle(style);
            return feature;
          });
        }).filter(function (feature) {
          return !_.isUndefined(feature);
        }).value());
      };
    };

})(this);