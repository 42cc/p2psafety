var mapApp = angular.module('mapApp', ["angular-lodash","ngAnimate"]);

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
        $scope.focus(event.latest_location);
        $scope.zoomIn();
        $scope.selectedEvent = event;
        window.location.hash = event.id;
        $scope.selectedEvent.isNew = false;
      });

      var supportEvents = _.filter($scope.events, {"type": "support"});
      _.forEach(supportEvents,function (support) {
        if (_.any(support.supported,{"id":event.id})){
          $scope.selectedEventsupport[support.id] = support;
        }
        });
      _.forEach(event.supported, function(supported){
        $scope.selectedEventsupported[supported.id] = $scope.events[supported.id];
      });

    };
  };
  $scope.update = function(options, callback) {
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
            if (options.highightNew){
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
      if (eventsAppeared && options.playSoundForNew)
        document.getElementById('audiotag').play();
      // Making map show all events
      if (options.centerMap) {
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
      if (eventsAppeared){
          $scope.updateUserAttrs(newEvents);
      }

      if ('function' === typeof callback) callback();
    });
  };

  $scope.updateUserAttrs = function(events){
    _.forEach(events, function(event){
      //clojure for ajax callback
      var clojr = function(pevent,field){
        return function(data){
          pevent.user[field] = data;
        }
      }

      $http.get(urls.user_roles,
        {params:{id:event.user.id}}).success(clojr(event,'roles'))
      $http.get(urls.user_movement_types,
        {params:{id:event.user.id}}).success(clojr(event,'movement_types'))
    });
  }

  $scope.focus = function(location) {
    $scope.gmap.panTo(new google.maps.LatLng(location.latitude,
                                             location.longitude));
  };

  $scope.getRoles = function() {
    //populate list of avail roles for matching
    $scope.filters.roles={};
    $http.get(urls.roles).success(function(data) {
      _.forEach(data.objects, function(role){
        role.enabled = true;
        $scope.filters.roles[role.id] = role;
      });
    });
  }

  $scope.getMovementTypes = function() {
    //populate list of avail movement_types for matching
    $scope.filters.movement_types={};
    $http.get(urls.movement_types).success(function(data) {
      _.forEach(data.objects, function(mt){
        mt.enabled = true;
        $scope.filters.movement_types[mt.id] = mt;
      });
    });
  }
  $scope.setFiltersTo=function(filters,value){
      //set array of user filters to be disabled or enabled, all at once
      _.map(filters,function(el){el.enabled=value});
  }

  $scope.userFilters = function(event) {
      // filter events for attrs event.user.role or event.user.movement_type
      // if user has no attr - do not filter him.
      roles = event.user.roles;
      movement_types = event.user.movement_types;
      //do not filter elements without attrs
      if (_.isEmpty(roles) && _.isEmpty(movement_types)) return true;

      var hasRoles = _.any(roles, function(role_id){
        return $scope.filters.roles[role_id].enabled
      });

      var hasMovementTypes = _.any(movement_types, function(mtype_id){
        return $scope.filters.movement_types[mtype_id].enabled
      });

      return (hasRoles&&hasMovementTypes)
  }

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

  $scope.updatePerSeconds = 5;
  $scope.selectedEvent = null;
  $scope.zoomedIn = false;
  $scope.zoomScale = 1;
  $scope.initGoogleMap(document.getElementById("map-canvas"));
  $scope.events = {};
  $scope.isNotifyingSupporters = false;
  $scope.showFilterPanel = false;
  $scope.fields = {
    addEventUpdateText: '',
    notifySupportersRadius: '',
  };

  $scope.filters = {};
  $scope.getRoles();
  $scope.getMovementTypes();
  $scope.update({playSoundForNew:false, highightNew:false, centerMap:true});

  $interval(function() {
    document.getElementById('audiotag').play();
    alert('Do You sleep?');
  }, mapSettings.wakeup_interval * 60 * 1000);

  $interval(function() {
    $scope.update({playSoundForNew:mapSettings.sound,
      highightNew:mapSettings.highlight,
      centerMap:false});
  }, $scope.updatePerSeconds * 1000);

  $interval(function() {
    $scope.updateUserAttrs($scope.events);
  },30*1000);
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
