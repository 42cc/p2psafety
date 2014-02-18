var mapApp = angular.module('mapApp', []);

mapApp.constant('ICONS', {
  RED: 'http://maps.google.com/mapfiles/ms/micons/red-dot.png',
  GREEN: 'http://maps.google.com/mapfiles/ms/micons/green-dot.png',
  BLUE: 'http://maps.google.com/mapfiles/ms/micons/blue-dot.png',
});

mapApp.controller('EventListCtrl', function($scope, $http, $interval, urls, mapSettings) {
  $scope.initGoogleMap = function(rootElement) {

    var fullBounds = new google.maps.LatLngBounds();
    var params = {status: 'A'};
    $http.get(urls.events, {params: params}).success(function(data) {
        for (i in data.objects) {
            var event = data.objects[i];
            var lat=event.latest_location.latitude;
            var long=event.latest_location.longitude;
            var point=new google.maps.LatLng(lat, long);
        fullBounds.extend(point)
        };
    });

    var mapOptions = {
      zoom: 10,
      center: new google.maps.LatLng(50.444, 390.56),
      zoomControl: true,
      zoomControlOptions: {position: google.maps.ControlPosition.RIGHT_CENTER},
      panControl: true,
      panControlOptions: {position: google.maps.ControlPosition.RIGHT_TOP},
    };
    $scope.gmap = new google.maps.Map(rootElement, mapOptions);
    $scope.gwindow = new google.maps.InfoWindow({content: "Sup"});
    $scope.gwindow_opened = false;
    $scope.gmap.fitBounds(fullBounds);
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
    var params = {status: 'A'};
    $http.get(urls.events, {params: params}).success(function(data) {
        var eventsAppeared = false, newEvents = {};
          for (i in data.objects) {
            var event = data.objects[i];
            newEvents[event.id] = event;
          }
          // Deleting old events
          for (oldEventId in $scope.events) {
            if (newEvents[oldEventId] == null) {
              delete $scope.events[oldEventId];
            }
          }
          // Adding new events
          for (newEventId in newEvents) {
            if ($scope.events[newEventId] == null) {
                var new_event =  newEvents[newEventId]
                if (mapSettings.highlight){
                    new_event.isNew = true
                } else {
                    new_event.isNew = false
                }
              $scope.events[newEventId] = new_event;
              eventsAppeared = true;
            }
    }
    if (eventsAppeared && mapSettings.sound) document.getElementById('audiotag').play();
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
.directive('eventMarker', function($http, ICONS) {
  var linker = function(scope, element, attrs) {
    var loc = scope.$eval(attrs.location);
    var markersWindow = null;

    if (loc != null) {
      var markerArgs = {
        position: new google.maps.LatLng(loc.latitude, loc.longitude),
      };

      var mode = (attrs.mode == null) ? 'event' : attrs.mode;
      if (mode == 'event') {
        var eventType = (attrs.type == null) ? 'victim' : attrs.type;
        if (eventType == 'victim') {
          markerArgs.icon = ICONS.RED;
        } else if (eventType == 'support') {
          markerArgs.icon = ICONS.GREEN;
        };
      } else if (mode == 'eventupdate') {
        markerArgs.icon = ICONS.BLUE;
      }

      var marker = new google.maps.Marker(markerArgs);
      marker.setMap(scope.$parent.gmap);

      if (attrs.bouncing=="true"){
        marker.setAnimation(google.maps.Animation.BOUNCE);
    } else {
        marker.setAnimation(null);
    }


      if (attrs.click) {
        google.maps.event.addListener(marker, 'click', function() {
          scope.$eval(attrs.click);
          marker.setAnimation(null);
        });
      }

      var hoverContent = element.children();
      if (hoverContent.length) {
        hoverContent = hoverContent.detach()[0];
        markersWindow = new google.maps.InfoWindow();
        
        google.maps.event.addListener(marker, 'mouseover', function() {
          markersWindow.setContent(hoverContent);
          markersWindow.open(scope.$parent.gmap, marker);
          marker.setAnimation(null);
        });
        google.maps.event.addListener(marker, 'mouseout', function() {
          if (markersWindow != null) markersWindow.close();
        });
      }

      element.on('$destroy', function() {
        marker.setMap(null);
        marker = null;
        if (markersWindow != null) {
          markersWindow.close();
          markersWindow = null;
        }
      });
    };
  };
  return {
    replace: true,
    template: '',
    restrict: 'E',
    link: linker,
  };
});
