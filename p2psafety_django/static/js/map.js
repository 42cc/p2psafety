var mapApp = angular.module('mapApp', []);

mapApp.constant('ICONS', {
  RED: 'http://maps.google.com/mapfiles/ms/micons/red-dot.png',
  GREEN: 'http://maps.google.com/mapfiles/ms/micons/green-dot.png',
  BLUE: 'http://maps.google.com/mapfiles/ms/micons/blue-dot.png',
});

mapApp.controller('EventListCtrl', function($scope, $http, $interval, urls, mapSettings) {
  $scope.$location = window.location
  $scope.selectedEventsupport = {}
  $scope.selectedEventsupported = {}
  $scope.initGoogleMap = function(rootElement) {

    var fullBounds = new google.maps.LatLngBounds();
    var params = {status: 'A'};
    $http.get(urls.events, {params: params}).success(function(data) {
      for (i in data.objects) {
        var event = data.objects[i];
        if (event.latest_location != null) {
          var point = new google.maps.LatLng(
            event.latest_location.latitude,
            event.latest_location.longitude
          );
          fullBounds.extend(point)
        }
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
      $scope.selectedEventsupported = {};
      window.location.hash = '';
    } else {
      var params = {event__id: event.id};
      $http.get(urls.eventupdates, {params: params}).success(function(data) {
        event.updates = data.objects;
        $scope.zoomIn();
        $scope.selectedEvent = event;
        window.location.hash = event.id;
        $scope.selectedEvent.isNew = false;
      });
      for (i in $scope.events) {
        var event_support = $scope.events[i];
        if(event_support.type=="support"){
          for (var i = 0; i<event_support.supported.length; i++){
            var supported_id = parseFloat(event_support.supported[i].split('/')[4]);
              var supported = $scope.events[supported_id];
              if(supported.id == event.id){
                  $scope.selectedEventsupport[event_support.id] = event_support;
              }
          }
        }
      }
      for (var i = 0; i<event.supported.length; i++) {
        var supported_id = parseFloat(event.supported[i].split('/')[4]);
        var supported = $scope.events[supported_id];
        $scope.selectedEventsupported[supported.id] = supported;
      }
    };
  };
  $scope.update = function(highightNew, playSoundForNew, centerMap) {
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

      // Making map show all events
      if (centerMap) {
        var eventsBounds = new google.maps.LatLngBounds();
        for (eventId in $scope.events) {
          var event = $scope.events[eventId];
          eventsBounds.extend(new google.maps.LatLng(
            event.latest_location.latitude,
            event.latest_location.longitude
          ));
        }
        $scope.gmap.fitBounds(eventsBounds);
      }
    });
  };
  $scope.focus = function(location) {
    $scope.gmap.panTo(new google.maps.LatLng(location.latitude,
                                             location.longitude));
  };

  $scope.addEventUpdate = function() {
    var event = $scope.selectedEvent,
        text = $scope.fields.addEventUpdateText,
        url = urls.addEventUpdate;
    $http.post(url, {"event_id":event.id, "text":text}).success(function(data) {
        var params = {event__id: event.id};
        $http.get(urls.eventupdates, {params: params}).success(function(data) {
            event.updates = data.objects;
        })
    })
    $scope.fields.addEventUpdateText = '';
  };
  $scope.ctrlEnter = function(event) {
    var ctrlPressed = event.metaKey || event.ctrlKey;
    var enterPressed = event.keyCode == 13;
    var text = $scope.fields.addEventUpdateText;
    if (ctrlPressed && enterPressed && text.length) {
      $scope.addEventUpdate(text);
    }
  };
  $scope.closeEvent = function(event) {
    $http.post(urls.closeEvent, {event_id: event.id}).success(function(data) {
      $scope.select(null);
      delete $scope.events[event.id];
    });
  };
  $scope.notifySupporters = function(event) {
    var data = {
      event_id: $scope.selectedEvent.id,
      radius: $scope.fields.notifySupportersRadius
    };
    $scope.isNotifyingSupporters = true;
    $http.post(urls.notifySupporters, data).success(function(data) {
      $scope.isNotifyingSupporters = false;
    })
  };

  setInterval(function() {
    document.getElementById('audiotag').play();
    alert('Do You sleep?');
  }, mapSettings.wakeup_interval * 60 * 1000);

  $scope.updatePerSeconds = 5;
  $scope.selectedEvent = null;
  $scope.zoomedIn = false;
  $scope.zoomScale = 1;
  $scope.initGoogleMap(document.getElementById("map-canvas"));
  $scope.events = {};
  $scope.fields = {
    addEventUpdateText: '',
    notifySupportersRadius: '',
  };

  $scope.isNotifyingSupporters = false;
  $scope.update(false, false, true);

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
    var content = element.children().detach()[0];
    var location = scope.event.latest_location;

    if (location) {
      var map = scope.$parent.gmap;
      var icon = (scope.event.type == 'victim') ? ICONS.RED : ICONS.GREEN;
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
.directive('supportMarker', function(markerFactory, ICONS) {
  var linker = function(scope, element, attrs) {
    var content = element.children().detach()[0];
    var location = scope.support.latest_location;

    if (location) {
      var map = scope.$parent.gmap;
      var marker = markerFactory(scope, element, content, ICONS.GREEN, location,
                                 map, attrs.click);
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
    var content = element.children().detach()[0];
    var location = scope.update.location != null;

    if (location) {
      var map = scope.$parent.gmap;
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
})
.directive('supportedMarker', function(markerFactory, ICONS) {
  var linker = function(scope, element, attrs) {
    var content = element.children().detach()[0];
    var location = scope.supported.latest_location;

    if (location) {
      var map = scope.$parent.gmap;
      var marker = markerFactory(scope, element, content, ICONS.RED, location,
                                 map, attrs.click);
    }
  };
  return {
    replace: true,
    template: '',
    restrict: 'E',
    link: linker,
  };
})
