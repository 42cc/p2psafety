var mapApp = angular.module('mapApp', []);

mapApp.controller('EventListCtrl', function($scope, $http, $interval, urls) {
  $scope.initGoogleMap = function(rootElement) {
    var mapOptions = {
      zoom: 11,
      center: new google.maps.LatLng(50.444, 390.56),
      zoomControl: true,
      zoomControlOptions: {position: google.maps.ControlPosition.RIGHT_CENTER},
      panControl: true,
      panControlOptions: {position: google.maps.ControlPosition.RIGHT_TOP},
    };
    $scope.gmap = new google.maps.Map(rootElement, mapOptions);
    $scope.gwindow = new google.maps.InfoWindow({content: "Sup"});
    $scope.gwindow_opened = false;
  };
  $scope.zoomIn = function() {
    if ($scope.zoomedIn == false) {
      $scope.gmap.setZoom($scope.gmap.getZoom() + $scope.zoomScale);
      $scope.zoomedIn = true;
    }
  };
  $scope.zoomOut = function() {
    if ($scope.zoomedIn == true) {
      $scope.gmap.setZoom($scope.gmap.getZoom() - $scope.zoomScale);
      $scope.zoomedIn = false;
    }
  };
  $scope.select = function(event) {
    if (event == null) {
      $scope.zoomOut();
      $scope.selectedEvent = null;
    } else {
      var params = {event__id: event.id};
      $http.get(urls.eventupdates, {params: params}).success(function(data) {
        event.updates = data.objects;
        $scope.zoomIn();
        $scope.selectedEvent = event;
      });
    };
  };
  $scope.update = function() {
    $http.get(urls.events).success(function(data) {
      for (i in data.objects) {
        var event = data.objects[i];
        // TODO: display it manually
        if (event.latest_location != null) {
          var existingEvent = $scope.events[event.id];
          if (existingEvent == null) {
            $scope.events[event.id] = event;
          } else {
            for (key in event) {
              var eventProperty = event[key];
              if (existingEvent[key] != eventProperty) {
                existingEvent[key] = eventProperty;
              }
            }
          }
        }
      };
    });
  };
  $scope.focus = function(location) {
    $scope.gmap.panTo(new google.maps.LatLng(location.latitude,
                                             location.longitude));
  };
  $scope.updatePerSeconds = 5;
  $scope.selectedEvent = null;
  $scope.zoomedIn = false;
  $scope.zoomScale = 1;
  $scope.initGoogleMap(document.getElementById("map-canvas"));
  $scope.events = {};
  
  $scope.update();
  $interval(function() {
    $scope.update();
  }, $scope.updatePerSeconds * 1000);
})
.directive('gmapMarker', function($http) {
  var linker = function(scope, element, attrs) {
    var loc = scope.$eval(attrs.location);

    if (loc != null) {
      var markerArgs = {
        position: new google.maps.LatLng(loc.latitude, loc.longitude)
      };
      if (attrs.icon) {
        markerArgs.icon = attrs.icon;
      }
      var marker = new google.maps.Marker(markerArgs);
      marker.setMap(scope.$parent.gmap);

      if (attrs.click) {
        google.maps.event.addListener(marker, 'click', function() {          
          scope.$eval(attrs.click);
        });
      }

      element.on('$destroy', function() {
        marker.setMap(null);
        marker = null;
      });
    };
  };
  return {
    replace: true,
    restrict: 'E',
    link: linker,
  };
});