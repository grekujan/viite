
var assert = chai.assert;

describe('BusStopLayerPlugin', function(){
    describe('#makePopupContent()', function() {
        var pluginInstance = null;

        var dataOneBusStopType = ["2"];
        var testOneBusStopTypeHtml =  '<img src="/api/images/2">';

        var dataTwoBusStopType = ["2","3"];
        var testTwoBusStopTypeHtml =  '<img src="/api/images/2"><img src="/api/images/3">';

        var dataEmptyBusStopType = [];
        var testEmptyBusStopTypeHtml =  '';

        before(function(){
            pluginInstance = Oskari.clazz.create('Oskari.digiroad2.bundle.mapbusstop.plugin.BusStopLayerPlugin');
            pluginInstance._initTemplates();
        });

        it('should return one bus stop html by image tag', function () {
            assert.equal(testOneBusStopTypeHtml, pluginInstance._makePopupContent(dataOneBusStopType));
        });

        it('should return two various bus stop html by image tags', function () {
            assert.equal(testTwoBusStopTypeHtml, pluginInstance._makePopupContent(dataTwoBusStopType));
        });

        it('should return empty html', function () {
            assert.equal(testEmptyBusStopTypeHtml, pluginInstance._makePopupContent(dataEmptyBusStopType));
        });
    });

    describe('when adding a new bus stop', function() {
        var pluginInstance = null;
        var request = null;
        var requestCallback = null;
        var assetCreationData = [];
        var attributeCollectionRequest = {};
        var attributeCollectionRequestBuilder = function(callback) {
            requestCallback = callback;
            return attributeCollectionRequest;
        };

        before(function() {
            pluginInstance = Oskari.clazz.create('Oskari.digiroad2.bundle.mapbusstop.plugin.BusStopLayerPlugin', {
                backend: _.extend({}, window.Backend, {
                    putAsset: function(data) {
                        assetCreationData.push(data);
                    }
                })
            });
            pluginInstance.setMapModule({
                getName: function() { return 'MapModule'; },
                getMap: function() { return {}; }
            });
            pluginInstance.startPlugin({
                register: function() {},
                registerForEventByName: function() {},
                getRequestBuilder: function(request) {
                    return request === 'FeatureAttributes.CollectFeatureAttributesRequest' ? attributeCollectionRequestBuilder : null;
                },
                request: function(name, r) { request = r; }
            });
            pluginInstance._toolSelectionChange({
                getAction: function() { return 'AddWithCollection'; }
            });
            pluginInstance._addBusStopEvent({});
        });

        it('should request collection of feature attributes', function() {
            assert.equal(request, attributeCollectionRequest);
        });

        describe('and when feature attributes have been collected', function () {
            before(function() {
                assetCreationData = [];
                requestCallback();
            });

            it('should create asset in back end', function () {
                assert.equal(1, assetCreationData.length);
                assert.deepEqual({ assetTypeId: 10, lon: 0, lat: 0, roadLinkId: 0, bearing: 0 }, assetCreationData[0]);
            });
        });
    });
});