var mapApp = angular.module('mapApp', []);

mapApp.constant('ICONS', {
  RED: 'http://maps.google.com/mapfiles/ms/micons/red-dot.png',
  GREEN: 'http://maps.google.com/mapfiles/ms/micons/green-dot.png',
  BLUE: 'http://maps.google.com/mapfiles/ms/micons/blue-dot.png',
});

mapApp.controller('EventListCtrl', function($scope, $http, $interval, urls, mapSettings) {
  $scope.$location = window.location
  $scope.selectedEventsupport = {}
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
      $scope.selectedEventsupport = {};
      window.location.hash = ''
    } else {
      var params = {event__id: event.id};
      $http.get(urls.eventupdates, {params: params}).success(function(data) {
      event.updates = data.objects;
      $scope.zoomIn();
      $scope.selectedEvent = event;
      window.location.hash = event.id
      $scope.selectedEvent.isNew = false;
      });
        var params = {status: 'A'};
        $http.get(urls.events, {params: params}).success(function(data) {
          for (i in data.objects) {
              var event_support = data.objects[i];
              if(event_support.type=="support"){
                  for (i in event_support.supported){
                    var support = event_support.supported[i]
                    if(support.id==$scope.selectedEvent.id){
                      if ($scope.selectedEventsuppor == null){
                        $scope.selectedEventsupport[event_support.id] = event_support;
                      }else if ($scope.selectedEventsupport[event_support.id] == null){
                        $scope.selectedEventsupport[event_support.id] = event_support;
                      }
                    }
                  }
              }
            }
        });
    };
  };
  $scope.update = function(highightNew, playSoundForNew) {
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
            if (highightNew){
              new_event.isNew = true
            } else {
              new_event.isNew = false
            }
          $scope.events[newEventId] = new_event;
          eventsAppeared = true;
        }
      }
      if ($scope.$location.hash!=""){
          var id = parseFloat($scope.$location.hash.split('#')[1])
          $scope.select( $scope.events[id])
        }
      if (eventsAppeared && playSoundForNew)
        document.getElementById('audiotag').play();
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
  
  $scope.update(false, false);

  $interval(function() {
    $scope.update(mapSettings.highlight, mapSettings.sound);
  }, $scope.updatePerSeconds * 1000);
})
.factory('markerFactory', function() {
  return function(scope, element, content, icon, location, map, onclick) {    
    var markerArgs = {
      icon: icon,
      position: new google.maps.LatLng(location.latitude, location.longitude),
    };

    var marker = new google.maps.Marker(markerArgs);
    marker.setMap(map);
    if (onclick)
      google.maps.event.addListener(marker, 'click', function() {
        scope.$eval(onclick);
      });

    if (content.length) {
      content = content.detach()[0];
    }
    var markersWindow = new google.maps.InfoWindow();
      
    google.maps.event.addListener(marker, 'mouseover', function() {
      markersWindow.setContent(content);
      markersWindow.open(map, marker);
    });
    google.maps.event.addListener(marker, 'mouseout', function() {
      if (markersWindow != null) markersWindow.close();
    });

    element.on('$destroy', function() {
      marker.setMap(null);
      marker = null;
      if (markersWindow != null) {
        markersWindow.close();
        markersWindow = null;
      }
    });

    return marker;
  };
})
.directive('eventMarker', function(markerFactory, ICONS) {
  var linker = function(scope, element, attrs) {
    var location = scope.event.latest_location;

    if (location) {
      var map = scope.$parent.gmap;
      var icon = (scope.event.type == 'victim') ? ICONS.RED : ICONS.GREEN;
      var content = element.children().detach()[0];
      var marker = markerFactory(scope, element, content, icon, location,
                                 map, attrs.click);

      scope.$watch('event.isNew', function(isNew) {
        var animation = (isNew) ? google.maps.Animation.BOUNCE : null;
        marker.setAnimation(animation);
      });
    }
  };
  return {
    replace: true,
    template: '',
    restrict: 'E',
    link: linker,
  };
})
.directive('eventupdateMarker', function(markerFactory, ICONS) {
  var linker = function(scope, element, attrs) {
    if (scope.update.location != null) {
      var map = scope.$parent.gmap;
      var content = element.children().detach()[0];
      var marker = markerFactory(scope, element, content, ICONS.BLUE,
                                 scope.update.location, map);
    }
  };
  return {
    replace: true,
    template: '',
    restrict: 'E',
    link: linker,
  };
});
