(function (root) {
  root.RoadLinkStyler = function () {
    /**
     * RoadLinkstyler is styler for normal roadlinks in projectmode for setting them opacity. Does not include linedashes since we are not sure if those will be included in project mode
     */

    var strokeWidthRules = [
        new StyleRule().where('zoomLevel').is(5).use({stroke: {width: 4 }}),
        new StyleRule().where('zoomLevel').is(6).use({stroke: {width: 4 }}),
        new StyleRule().where('zoomLevel').is(7).use({stroke: {width: 4 }}),
        new StyleRule().where('zoomLevel').is(8).use({stroke: {width: 5 }}),
        new StyleRule().where('zoomLevel').is(9).use({stroke: {width: 5 }}),
        new StyleRule().where('zoomLevel').is(10).use({stroke: {width: 5 }}),
        new StyleRule().where('zoomLevel').is(11).use({stroke: {width: 5 }}),
        new StyleRule().where('zoomLevel').is(12).use({stroke: {width: 8 }}),
        new StyleRule().where('zoomLevel').is(13).use({stroke: {width: 8 }}),
        new StyleRule().where('zoomLevel').is(14).use({stroke: {width: 10 }}),
        new StyleRule().where('zoomLevel').is(15).use({stroke: {width: 10 }})
    ];

      var strokeAdministrativeClassRules = [
          new StyleRule().where('zoomLevel').is(5).and('administrativeClass').is('Municipality').and('roadClass').isNot(7)
              .and('roadClass').isNot(8).and('roadClass').isNot(9).and('roadClass').isNot(99)
              .or('administrativeClass').is('Private').and('roadClass').isNot(7).and('roadClass').isNot(8).and('roadClass').isNot(9)
              .and('roadClass').isNot(99).use({stroke: {width: 7 }}),
          new StyleRule().where('zoomLevel').is(6).and('administrativeClass').is('Municipality').and('roadClass').isNot(7)
              .and('roadClass').isNot(8).and('roadClass').isNot(9).and('roadClass').isNot(99)
              .or('administrativeClass').is('Private').and('roadClass').isNot(7).and('roadClass').isNot(8).and('roadClass').isNot(9)
              .and('roadClass').isNot(99).use({stroke: {width: 8 }}),
          new StyleRule().where('zoomLevel').is(7).and('administrativeClass').is('Municipality').and('roadClass').isNot(7)
              .and('roadClass').isNot(8).and('roadClass').isNot(9).and('roadClass').isNot(99)
              .or('administrativeClass').is('Private').and('roadClass').isNot(7).and('roadClass').isNot(8).and('roadClass').isNot(9)
              .and('roadClass').isNot(99).use({stroke: {width: 9 }}),
          new StyleRule().where('zoomLevel').is(8).and('administrativeClass').is('Municipality').and('roadClass').isNot(7)
              .and('roadClass').isNot(8).and('roadClass').isNot(9).and('roadClass').isNot(99)
              .or('administrativeClass').is('Private').and('roadClass').isNot(7).and('roadClass').isNot(8).and('roadClass').isNot(9)
              .and('roadClass').isNot(99).use({stroke: {width: 9 }}),
          new StyleRule().where('zoomLevel').is(9).and('administrativeClass').is('Municipality').and('roadClass').isNot(7)
              .and('roadClass').isNot(8).and('roadClass').isNot(9).and('roadClass').isNot(99)
              .or('administrativeClass').is('Private').and('roadClass').isNot(7).and('roadClass').isNot(8).and('roadClass').isNot(9)
              .and('roadClass').isNot(99).use({stroke: {width: 9 }}),
          new StyleRule().where('zoomLevel').is(10).and('administrativeClass').is('Municipality').and('roadClass').isNot(7)
              .and('roadClass').isNot(8).and('roadClass').isNot(9).and('roadClass').isNot(99)
              .or('administrativeClass').is('Private').and('roadClass').isNot(7).and('roadClass').isNot(8).and('roadClass').isNot(9)
              .and('roadClass').isNot(99).use({stroke: {width: 10 }}),
          new StyleRule().where('zoomLevel').is(11).and('administrativeClass').is('Municipality').and('roadClass').isNot(7)
              .and('roadClass').isNot(8).and('roadClass').isNot(9).and('roadClass').isNot(99)
              .or('administrativeClass').is('Private').and('roadClass').isNot(7).and('roadClass').isNot(8).and('roadClass').isNot(9)
              .and('roadClass').isNot(99).use({stroke: {width: 10 }}),
          new StyleRule().where('zoomLevel').is(12).and('administrativeClass').is('Municipality').and('roadClass').isNot(7)
              .and('roadClass').isNot(8).and('roadClass').isNot(9).and('roadClass').isNot(99)
              .or('administrativeClass').is('Private').and('roadClass').isNot(7).and('roadClass').isNot(8).and('roadClass').isNot(9)
              .and('roadClass').isNot(99).use({stroke: {width: 11 }}),
          new StyleRule().where('zoomLevel').is(13).and('administrativeClass').is('Municipality').and('roadClass').isNot(7)
              .and('roadClass').isNot(8).and('roadClass').isNot(9).and('roadClass').isNot(99)
              .or('administrativeClass').is('Private').and('roadClass').isNot(7).and('roadClass').isNot(8).and('roadClass').isNot(9)
              .and('roadClass').isNot(99).use({stroke: {width: 11 }}),
          new StyleRule().where('zoomLevel').is(14).and('administrativeClass').is('Municipality').and('roadClass').isNot(7)
              .and('roadClass').isNot(8).and('roadClass').isNot(9).and('roadClass').isNot(99)
              .or('administrativeClass').is('Private').and('roadClass').isNot(7).and('roadClass').isNot(8).and('roadClass').isNot(9)
              .and('roadClass').isNot(99).use({stroke: {width: 12 }}),
          new StyleRule().where('zoomLevel').is(15).and('administrativeClass').is('Municipality').and('roadClass').isNot(7)
              .and('roadClass').isNot(8).and('roadClass').isNot(9).and('roadClass').isNot(99)
              .or('administrativeClass').is('Private').and('roadClass').isNot(7).and('roadClass').isNot(8).and('roadClass').isNot(9)
              .and('roadClass').isNot(99).use({stroke: {width: 12 }})
      ];

      var fillWidthRules = [
          new StyleRule().where('zoomLevel').is(5).use({stroke: {width: 1 }}),
          new StyleRule().where('zoomLevel').is(6).use({stroke: {width: 2 }}),
          new StyleRule().where('zoomLevel').is(7).use({stroke: {width: 2 }}),
          new StyleRule().where('zoomLevel').is(8).use({stroke: {width: 3 }}),
          new StyleRule().where('zoomLevel').is(9).use({stroke: {width: 3 }}),
          new StyleRule().where('zoomLevel').is(10).use({stroke: {width: 3 }}),
          new StyleRule().where('zoomLevel').is(11).use({stroke: {width: 3 }}),
          new StyleRule().where('zoomLevel').is(12).use({stroke: {width: 6 }}),
          new StyleRule().where('zoomLevel').is(13).use({stroke: {width: 6 }}),
          new StyleRule().where('zoomLevel').is(14).use({stroke: {width: 8 }}),
          new StyleRule().where('zoomLevel').is(15).use({stroke: {width: 8 }})
      ];

    var strokeRules = [
        new StyleRule().where('roadClass').is(1).use({stroke: {color: '#FF0000', lineCap: 'round'}}),
        new StyleRule().where('roadClass').is(2).use({stroke: {color: '#FFD76A',  lineCap: 'round'}}),
        new StyleRule().where('roadClass').is(3).use({stroke: {color: '#FFD76A',  lineCap: 'round'}}),
        new StyleRule().where('roadClass').is(4).use({stroke: {color: '#0011BB',  lineCap: 'round'}}),
        new StyleRule().where('roadClass').is(5).use({stroke: {color: '#33CCCC',  lineCap: 'round'}}),
        new StyleRule().where('roadClass').is(6).use({stroke: {color: '#E01DD9',  lineCap: 'round'}}),
        new StyleRule().where('roadClass').is(7).use({stroke: {color: '#00CCDD',  lineCap: 'round'}}),
        new StyleRule().where('roadClass').is(8).use({stroke: {color: '#FC6DA0', lineCap: 'round'}}),
        new StyleRule().where('roadClass').is(9).use({stroke: {color: '#FF55DD', lineCap: 'round'}}),
        new StyleRule().where('roadClass').is(10).use({stroke: {color: '#FF55DD', lineCap: 'round'}}),
        new StyleRule().where('roadClass').is(11).use({stroke: {color: '#444444', lineCap: 'round'}}),
        new StyleRule().where('roadClass').is(97).use({stroke: {color: '#1E1E1E', lineCap: 'round'}}),
        new StyleRule().where('roadClass').is(98).use({stroke: {color: '#FAFAFA', lineCap: 'round'}}),
        new StyleRule().where('constructionType').is(LinkValues.ConstructionType.UnderConstruction.value).use({stroke: {color: '#ff9900', lineCap: 'round'}}),
        new StyleRule().where('gapTransfering').is(true).use({stroke: {color: '#00FF00', lineCap: 'round'}}),
        new StyleRule().where('administrativeClass').is('Municipality').and('roadClass').isNot(7).and('roadClass').isNot(8).and('roadClass').isNot(9).and('roadClass').isNot(99).use({stroke: {color: '#1E1E1E', lineCap: 'round'}}),
        new StyleRule().where('administrativeClass').is('Private').and('roadClass').isNot(7).and('roadClass').isNot(8).and('roadClass').isNot(9).and('roadClass').isNot(99).use({stroke: {color: '#1E1E1E', lineCap: 'round'}}),
        new StyleRule().where('roadClass').is(99).and('roadLinkSource').isNot(3).use({stroke: {color: '#A4A4A2', lineCap: 'round'}}),
        new StyleRule().where('roadClass').is(99).and('anomaly').is(1).use({stroke: {color: '#1E1E1E', lineCap: 'round'}}),
        new StyleRule().where('roadLinkSource').is(3).and('roadClass').is(99).use({stroke: {color: '#D3AFF6', lineCap: 'round'}})
    ];

    var fillRules = [
       new StyleRule().where('roadClass').is(1).use({stroke: {color: '#FF0000', lineCap: 'round'}}),
       new StyleRule().where('roadClass').is(2).use({stroke: {color: '#FF6600',  lineCap: 'round'}}),
       new StyleRule().where('roadClass').is(3).use({stroke: {color: '#FF9933',  lineCap: 'round'}}),
       new StyleRule().where('roadClass').is(4).use({stroke: {color: '#0011BB',  lineCap: 'round'}}),
       new StyleRule().where('roadClass').is(5).use({stroke: {color: '#33CCCC',  lineCap: 'round'}}),
       new StyleRule().where('roadClass').is(6).use({stroke: {color: '#E01DD9',  lineCap: 'round'}}),
       new StyleRule().where('roadClass').is(7).use({stroke: {color:'#fff', lineCap: 'butt', lineDash: [10, 10]}}),
       new StyleRule().where('roadClass').is(8).use({stroke: {color:'#fff', lineCap: 'butt', lineDash: [10, 10]}}),
       new StyleRule().where('roadClass').is(9).use({stroke: {color:'#fff', lineCap: 'butt', lineDash: [10, 10]}}),
       new StyleRule().where('roadClass').is(10).use({stroke: {color: '#FF55DD', lineCap: 'round'}}),
       new StyleRule().where('roadClass').is(11).use({stroke: {color: '#444444', lineCap: 'round'}}),
       new StyleRule().where('roadClass').is(97).use({stroke: {color: '#1E1E1E', lineCap: 'round'}}),
       new StyleRule().where('roadClass').is(98).use({stroke: {color: '#FAFAFA', lineCap: 'round'}}),
       new StyleRule().where('constructionType').is(LinkValues.ConstructionType.UnderConstruction.value).use({stroke: {color: '#ff9900', lineCap: 'round'}}),
       new StyleRule().where('gapTransfering').is(true).use({stroke: {color: '#00FF00', lineCap: 'round'}}),
       new StyleRule().where('roadClass').is(99).and('roadLinkSource').isNot(3).use({stroke: {color: '#A4A4A2', lineCap: 'round'}}),
       new StyleRule().where('roadClass').is(99).and('anomaly').is(1).use({stroke: {color: '#1E1E1E', lineCap: 'round'}}),
       new StyleRule().where('roadLinkSource').is(3).and('roadClass').is(99).use({stroke: {color: '#D3AFF6', lineCap: 'round'}})
    ];

    var roadLinkStyle = new StyleRuleProvider({});
    roadLinkStyle.addRules(strokeRules);
    roadLinkStyle.addRules(strokeWidthRules);
    roadLinkStyle.addRules(strokeAdministrativeClassRules);

    var overlayStyle = new StyleRuleProvider({zIndex: LinkValues.RoadZIndex.HistoricRoadLayer.value});
    overlayStyle.addRules(fillRules);
    overlayStyle.addRules(fillWidthRules);

    var getRoadLinkStyle = function () {
      return roadLinkStyle;
    };
    var getOverlayStyle = function () {
      return overlayStyle;
    };

    return {
      getOverlayStyle:getOverlayStyle,
      getRoadLinkStyle: getRoadLinkStyle

    };
  };
})(this);
